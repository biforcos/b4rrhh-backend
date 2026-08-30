package com.b4rrhh.shared.infrastructure.web.language;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Un controlador que declara un parámetro {@link ResponseLanguage} lo recibe ya resuelto
 * desde {@code Accept-Language}. Los tests de controlador con {@code standaloneSetup} lo
 * registran con {@code setCustomArgumentResolvers}.
 */
public class ResponseLanguageArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ResponseLanguage.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        return ResponseLanguage.fromAcceptLanguage(webRequest.getHeader(HttpHeaders.ACCEPT_LANGUAGE));
    }
}
