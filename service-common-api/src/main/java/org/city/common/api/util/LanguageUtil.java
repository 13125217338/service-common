package org.city.common.api.util;

import java.util.List;
import java.util.Properties;

import org.city.common.api.in.util.Language;

/**
 * @作者 ChengShi
 * @日期 2024-09-16 11:15:12
 * @版本 1.0
 * @描述 多语言工具
 */
public final class LanguageUtil {
	private LanguageUtil() {}
	private final static List<Language> LANGUAGES = SpringUtil.getBeans(Language.class); //多语言实现者
	
	/**
	 * @描述 获取多语言属性配置
	 * @param language 多语言（NULL=由接口实现者提供）
	 * @return 多语言属性配置
	 */
	public static Properties getLanguage(String language) {
		Properties languagePt = new Properties();
		for (Language lgg : LANGUAGES) {
			Properties lggPt = lgg.getLanguage(language);
			if (lggPt != null) {languagePt.putAll(lggPt);}
		}
		return languagePt;
	}
	
	/**
	 * @描述 获取多语言属性配置（反向翻译）
	 * @param language 多语言（NULL=由接口实现者提供）
	 * @return 多语言属性配置（反向翻译）
	 */
	public static Properties getLanguageX(String language) {
		Properties languagePt = new Properties();
		for (Language lgg : LANGUAGES) {
			Properties lggPt = lgg.getLanguage(language);
			if (lggPt != null) {
				lggPt.entrySet().forEach(v -> {
					languagePt.put(v.getValue(), v.getKey());
				});
			}
		}
		return languagePt;
	}
	
	/**
	 * @描述 翻译文本信息（将translating翻译成language语言）
	 * @param language 待翻译语言（NULL=由接口实现者提供）
	 * @param translating 待翻译文本
	 * @param defaultValue 未翻译默认值
	 * @return 翻译后的文本信息
	 */
	public static String translation(String language, String translating, String defaultValue) {
		return getLanguage(language).getProperty(translating, defaultValue);
	}
	
	/**
	 * @描述 反向翻译文本信息（将translated翻译成language反向语言）
	 * @param language 反向待翻译语言（NULL=由接口实现者提供）
	 * @param translated 翻译后的文本信息
	 * @param defaultValue 未翻译默认值
	 * @return 反向翻译后的文本信息
	 */
	public static String translationX(String language, String translated, String defaultValue) {
		return getLanguageX(language).getProperty(translated, defaultValue);
	}
}
