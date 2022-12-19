/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.lckp.anitomyJ;

import static com.lckp.anitomyJ.Element.ElementCategory.kElementAnimeSeasonPrefix;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementAnimeTitle;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementAnimeType;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementAnimeYear;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementEpisodeNumber;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementEpisodePrefix;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementEpisodeTitle;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementFileChecksum;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementReleaseGroup;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementReleaseVersion;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementUnknown;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementVideoResolution;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementVolumeNumber;
import static com.lckp.anitomyJ.Element.ElementCategory.kElementVolumePrefix;
import static com.lckp.anitomyJ.Token.TokenCategory.kBracket;
import static com.lckp.anitomyJ.Token.TokenCategory.kIdentifier;
import static com.lckp.anitomyJ.Token.TokenCategory.kUnknown;
import static com.lckp.anitomyJ.Token.TokenFlag.kFlagBracket;
import static com.lckp.anitomyJ.Token.TokenFlag.kFlagEnclosed;
import static com.lckp.anitomyJ.Token.TokenFlag.kFlagIdentifier;
import static com.lckp.anitomyJ.Token.TokenFlag.kFlagNone;
import static com.lckp.anitomyJ.Token.TokenFlag.kFlagNotDelimiter;
import static com.lckp.anitomyJ.Token.TokenFlag.kFlagNotEnclosed;
import static com.lckp.anitomyJ.Token.TokenFlag.kFlagUnknown;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;

import com.lckp.anitomyJ.Element.ElementCategory;
import com.lckp.anitomyJ.KeywordManager.KeywordOptions;
import com.lckp.anitomyJ.Token.Result;
import com.lckp.anitomyJ.Token.TokenFlag;

/**
 * Class to classify {@link Token}s.
 *
 * @author Paul Miller
 * @author Eren Okka
 */
public class Parser {
    private boolean isEpisodeKeywordsFound = false;
    private final ParserHelper parserHelper;
    private final ParserNumber parserNumber;
    private final List<Element> elements;
    private final List<Token> tokens;
    private final Options options;

    /**
     * Constructs a new token parser.
     *
     * @param elements the list where parsed elements will be added
     * @param options  the parser options
     * @param tokens   the list of tokens.
     */
    public Parser(List<Element> elements, Options options, List<Token> tokens) {
        this.elements = Objects.requireNonNull(elements);
        this.options = Objects.requireNonNull(options);
        this.tokens = Objects.requireNonNull(tokens);
        this.parserHelper = new ParserHelper(this);
        this.parserNumber = new ParserNumber(this);
    }

    /** Returns the list of elements. */
    public List<Element> getElements() {
        return elements;
    }

    /** Returns the list of tokens. */
    public List<Token> getTokens() {
        return tokens;
    }

    /** Returns the parser helper. */
    public ParserHelper getParserHelper() {
        return parserHelper;
    }

    /** Returns the number parser. */
    public ParserNumber getParserNumber() {
        return parserNumber;
    }

    /** Returns whether or not episode keywords were found. */
    public boolean isEpisodeKeywordsFound() {
        return isEpisodeKeywordsFound;
    }

    /** Begins the parsing process */
    public boolean parse() {
        searchForKeywords();
        searchForIsolatedNumbers();

        if (options.parseEpisodeNumber) {
            SearchForEpisodeNumber();
        }

        searchForAnimeTitle();

        if (options.parseReleaseGroup && empty(kElementReleaseGroup)) {
            searchForReleaseGroup();
        }

        if (options.parseEpisodeTitle && !empty(kElementEpisodeNumber)) {
            searchForEpisodeTitle();
        }

        validateElements();
        return empty(kElementAnimeTitle);
    }

    /** Search for anime keywords. */
    private void searchForKeywords() {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getCategory() != kUnknown) continue;

            String word = token.getContent();
            word = StringHelper.trimAny(word, " -");
            if (word.isEmpty()) continue;

            // Don't bother if the word is a number that cannot be CRC
            if (word.length() != 8 && StringHelper.isNumericString(word)) continue;

            String keyword = KeywordManager.normalzie(word);
            AtomicReference<ElementCategory> category = new AtomicReference<>(kElementUnknown);
            AtomicReference<KeywordOptions> options = new AtomicReference<>(new KeywordOptions());

            if (KeywordManager.getInstance().findAndSet(keyword, category, options)) {
                if (!this.options.parseReleaseGroup && category.get() == kElementReleaseGroup)
                    continue;
                if (!ParserHelper.isElementCategorySearchable(category.get()) || !options.get().isSearchable())
                    continue;
                if (ParserHelper.isElementCategorySingular(category.get()) && !empty(category.get()))
                    continue;
                if (category.get() == kElementAnimeSeasonPrefix) {
                    parserHelper.checkAndSetAnimeSeasonKeyword(token, i);
                    continue;
                } else if (category.get() == kElementEpisodePrefix) {
                    if (options.get().isValid()) {
                        parserHelper.checkExtentKeyword(kElementEpisodeNumber, i, token);
                        continue;
                    }
                } else if (category.get() == kElementReleaseVersion) {
                    word = StringUtils.substring(word, 1);
                } else if (category.get() == kElementVolumePrefix) {
                    parserHelper.checkExtentKeyword(kElementVolumeNumber, i, token);
                    continue;
                }
            } else {
                if (empty(kElementFileChecksum) && ParserHelper.isCrc32(word)) {
                    category.set(kElementFileChecksum);
                } else if (empty(kElementVideoResolution) && ParserHelper.isResolution(word)) {
                    category.set(kElementVideoResolution);
                }
            }

            if (category.get() != kElementUnknown) {
                elements.add(new Element(category.get(), word));
                if (options.get() != null && options.get().isIdentifiable()) {
                    token.setCategory(kIdentifier);
                }
            }
        }
    }

    /** Search for episode number. */
    private void SearchForEpisodeNumber() {
        // List all unknown tokens that contain a number
        List<Result> tokens = new ArrayList<>();
        for (int i = 0; i < this.tokens.size(); i++) {
            Token token = this.tokens.get(i);
            if (token.getCategory() == kUnknown && ParserHelper.indexOfFirstDigit(token.getContent()) != -1) {
                tokens.add(new Result(token, i));
            }
        }

        if (tokens.isEmpty()) return;

        isEpisodeKeywordsFound = !empty(kElementEpisodeNumber);

        // If a token matches a known episode pattern, it has to be the episode number
        if (parserNumber.searchForEpisodePatterns(tokens)) return;

        // We have previously found an episode number via keywords
        if (!empty(kElementEpisodeNumber)) return;

        // From now on, we're only interested in numeric tokens
        tokens.removeIf(r -> !StringHelper.isNumericString(r.token.getContent()));

        // e.g. "01 (176)", "29 (04)"
        if (parserNumber.searchForEquivalentNumbers(tokens)) return;

        // e.g. " - 08"
        if (parserNumber.searchForSeparatedNumbers(tokens)) return;

        // e.g. "[12]", "(2006)"
        if (parserNumber.searchForIsolatedNumbers(tokens)) return;

        // Consider using the last number as a last resort
        parserNumber.searchForLastNumber(tokens);
    }

    /** Search for anime title. */
    private void searchForAnimeTitle() {
        boolean enclosedTitle = false;

        Result tokenBegin = Token.findToken(tokens, kFlagNotEnclosed, kFlagUnknown);

        // If that doesn't work, find the first unknown token in the second enclosed
        // group, assuming that the first one is the release group
        if (tokenBegin.token == null) {
            tokenBegin = new Result(null, 0);
            enclosedTitle = true;
            boolean skippedPreviousGroup = false;

            do {
                tokenBegin = Token.findToken(tokens, tokenBegin, kFlagUnknown);
                if (tokenBegin.token == null) break;

                // Ignore groups that are composed of non-Latin characters
                if (StringHelper.isMostlyLatinString(tokenBegin.token.getContent()) && skippedPreviousGroup) {
                    break;
                }

                // Get the first unknown token of the next group
                tokenBegin = Token.findToken(tokens, tokenBegin, kFlagBracket);
                tokenBegin = Token.findToken(tokens, tokenBegin, kFlagUnknown);
                skippedPreviousGroup = true;
            } while (tokenBegin.token != null);
        }

        if (tokenBegin.token == null) return;

        // Continue until an identifier (or a bracket, if the title is enclosed)
        // is found
        Result tokenEnd = Token.findToken(tokens,
                                           tokenBegin,
                                           kFlagIdentifier,
                                           enclosedTitle ? kFlagBracket : kFlagNone);

        // If within the interval there's an open bracket without its matching pair,
        // move the upper endpoint back to the bracket
        if (!enclosedTitle) {
            int end = tokenEnd.pos != null ? tokenEnd.pos : tokens.size();
            Result lastBracket = tokenEnd;
            boolean bracketOpen = false;
            for (int i = tokenBegin.pos; i < end; i++) {
                Token token = tokens.get(i);
                if (token.getCategory() == kBracket) {
                    lastBracket = new Result(token, i);
                    bracketOpen = !bracketOpen;
                }
            }
            if (bracketOpen) tokenEnd = lastBracket;
        }

        // If the interval ends with an enclosed group (e.g. "Anime Title [Fansub]"),
        // move the upper endpoint back to the beginning of the group. We ignore
        // parentheses in order to keep certain groups (e.g. "(TV)") intact.
        if (!enclosedTitle) {
            int end = tokenEnd.pos != null ? tokenEnd.pos : tokens.size();
            Result token = Token.findPrevToken(tokens, end, kFlagNotDelimiter);

            while (ParserHelper.isTokenCategory(token.token, kBracket)
                    && token.token.getContent().charAt(0) != ')') {

                token = Token.findPrevToken(tokens, token, kFlagBracket);
                if (token.pos != null) {
                    tokenEnd = token;
                    token = Token.findPrevToken(tokens, tokenEnd, kFlagNotDelimiter);
                }
            }
        }

        int end = tokens.size();
        if (tokenEnd.token != null) end = Math.min(tokenEnd.pos, end);
        parserHelper.buildElement(kElementAnimeTitle, false, tokens.subList(tokenBegin.pos, end));
    }

    /** Search for release group. */
    private void searchForReleaseGroup() {
        for (Result tokenBegin = new Result(null, 0), tokenEnd = tokenBegin;
             tokenBegin.pos != null && tokenBegin.pos < tokens.size(); ) {

            // Find the first enclosed unknown token
            tokenBegin = Token.findToken(tokens, tokenEnd, kFlagEnclosed, kFlagUnknown);
            if (tokenBegin.token == null) return;

            // Continue until a bracket or identifier is found
            tokenEnd = Token.findToken(tokens, tokenBegin, kFlagBracket, kFlagIdentifier);
            if (tokenEnd.token == null || tokenEnd.token.getCategory() != kBracket) continue;

            // Ignore if it's not the first non-delimiter token in group
            Result prevToken = Token.findPrevToken(tokens, tokenBegin, TokenFlag.kFlagNotDelimiter);
            if (prevToken.token != null && prevToken.token.getCategory() != kBracket) continue;

            int end = tokens.size();
            end = Math.min(tokenEnd.pos, end);
            parserHelper.buildElement(kElementReleaseGroup, true, tokens.subList(tokenBegin.pos, end));
            return;
        }
    }

    /** Search for episode title. */
    private void searchForEpisodeTitle() {
        // Find the first non-enclosed unknown token
        Result tokenBegin = Token.findToken(tokens, kFlagNotEnclosed, kFlagUnknown);
        if (tokenBegin.token == null) return;

        // Continue until a bracket or identifier is found
        Result tokenEnd = Token.findToken(tokens, tokenBegin, kFlagBracket, kFlagIdentifier);

        int end = tokens.size();
        if (tokenEnd.pos != null) end = Math.min(tokenEnd.pos, end);
        parserHelper.buildElement(kElementEpisodeTitle, false, tokens.subList(tokenBegin.pos, end));
    }

    /** Search for isolated numbers. */
    private void searchForIsolatedNumbers() {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getCategory() != kUnknown
                    || !StringHelper.isNumericString(token.getContent()) || !parserHelper.isTokenIsolated(i)) {
                continue;
            }

            int number = StringHelper.stringToInt(token.getContent());

            // Anime year
            if (number >= ParserNumber.kAnimeYearMin && number <= ParserNumber.kAnimeYearMax) {
                if (empty(kElementAnimeYear)) {
                    elements.add(new Element(kElementAnimeYear, token.getContent()));
                    token.setCategory(kIdentifier);
                    continue;
                }
            }

            // Video resolution
            if (number == 480 || number == 720 || number == 1080) {
                // If these numbers are isolated, it's more likely for them to be the
                // video resolution rather than the episode number. Some fansub groups use these without the "p" suffix.
                if (empty(kElementVideoResolution)) {
                    elements.add(new Element(kElementVideoResolution, token.getContent()));
                    token.setCategory(kIdentifier);
                }
            }
        }
    }

    /** Validate Elements. */
    private void validateElements() {
        if (!empty(kElementAnimeType) && !empty(kElementEpisodeTitle)) {
            String episodeTitle = get(kElementEpisodeTitle);

            for (int i = 0; i < elements.size(); ) {
                Element el = elements.get(i);

                if (el.getCategory() == kElementAnimeType) {
                    if (StringUtils.contains(episodeTitle, el.getValue())) {
                        if (episodeTitle.length() == el.getValue().length()) {
                            elements.removeIf(element -> element.getCategory() == kElementEpisodeTitle); // invalid episode title
                        } else {
                            String keyword = KeywordManager.normalzie(el.getValue());
                            if (KeywordManager.getInstance().contains(kElementAnimeType, keyword)) {
                                i = erase(el);  // invalid anime type
                                continue;
                            }
                        }
                    }
                }
                ++i;
            }
        }
    }

    /** Returns whether or not the parser contains this category. */
    private boolean empty(ElementCategory category) {
        return !elements.stream().anyMatch(element -> element.getCategory() == category);
    }

    /** Returns the value of a particular category. */
    private String get(ElementCategory category) {
        Element foundElement = elements.stream()
                .filter(element -> element.getCategory() == category)
                .findAny()
                .orElse(null);

        if (foundElement == null) {
            Element e = new Element(category, "");
            elements.add(e);
            foundElement = e;
        }

        return foundElement.getValue();
    }

    /** Deletes the first element with the same {@code element.category} and returns the deleted elements position. */
    private int erase(Element element) {
        int removedIdx = -1;
        for (ListIterator<Element> itr = elements.listIterator(); itr.hasNext(); ) {
            int idx = itr.nextIndex();
            Element curE = itr.next();
            if (element.getCategory() == curE.getCategory()) {
                removedIdx = idx;
                itr.remove();
                break;
            }
        }

        return removedIdx;
    }
}
