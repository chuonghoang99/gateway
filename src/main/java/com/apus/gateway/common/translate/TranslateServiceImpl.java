package com.apus.gateway.common.translate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@Slf4j
public class TranslateServiceImpl implements TranslateService {
  private final MessageSource messageSource;

  @Autowired
  public TranslateServiceImpl(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @Override
  public String translate(String messageCode) {
    try {
      return this.messageSource.getMessage(messageCode, null, LocaleContextHolder.getLocale());
    } catch (Exception exception) {
      loggingError(messageCode, exception);
      return messageCode;
    }
  }

  @Override
  public String translateWithArgs(String messageCode, Object... args) {
    try {
      return this.messageSource.getMessage(messageCode, args, LocaleContextHolder.getLocale());
    } catch (Exception exception) {
      loggingError(messageCode, exception);
      return messageCode;
    }
  }

  @Override
  public String translateWithLang(String messageCode, String lang) {
    try {
      return this.messageSource.getMessage(messageCode, null, new Locale(lang));
    } catch (Exception exception) {
      loggingError(messageCode, exception);
      return messageCode;
    }
  }

  @Override
  public String translateWithLangAndArgs(String messageCode, String lang, Object... args) {
    try {
      return this.messageSource.getMessage(messageCode, args, new Locale(lang));
    } catch (Exception exception) {
      loggingError(messageCode, exception);
      return messageCode;
    }
  }

  private void loggingError(String messageCode, Exception exception) {
    log.warn("Translate {} has error", messageCode, exception);
  }
}
