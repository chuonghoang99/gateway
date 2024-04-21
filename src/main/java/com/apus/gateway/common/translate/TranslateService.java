package com.apus.gateway.common.translate;

public interface TranslateService {

  String translate(String messageCode);

  String translateWithArgs(String messageCode, Object... args);

  String translateWithLang(String messageCode, String lang);

  String translateWithLangAndArgs(String messageCode, String lang, Object... args);
}
