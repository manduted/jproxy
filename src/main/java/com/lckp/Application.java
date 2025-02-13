/**  
* @Title: Application.java
* @version V1.0  
*/
package com.lckp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * @ClassName: Application
 * @Description: jproxy启动类
 * @author LuckyPuppy514
 * @date 2020年6月18日 下午2:47:20
 *
 */
@SpringBootApplication
@MapperScan("com.lckp.mapper")
@ServletComponentScan
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
