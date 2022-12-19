package com.lckp.anitomyJ;

/*
@author manduted
@date 2022-12-09
*/

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class main {
    public static void main(String args[]) {
        String file = "[爱恋&漫猫字幕组]【契约之吻】Engage Kiss[S01E01-S01E13][1080P][MP4][GB][简中]";
        List<Element> elements = AnitomyJ.parse(file);
        List list;
        System.out.println(elements.stream()
                .map(e -> e.getCategory().name() + "=" + e.getValue())
                .collect(Collectors.toList()));
        list = (elements.stream()
                .map(e -> e.getCategory().name() + "=" + e.getValue())
                .collect(Collectors.toList()));
        System.out.println(list);
    }
}
