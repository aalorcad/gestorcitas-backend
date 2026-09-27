package cl.duoc.gestorcitas.usuarios.config;

import cl.duoc.gestorcitas.usuarios.dto.UsuarioContexto;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class UsuarioContextoArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String H_ID = "X-User-Id";
    public static final String H_NOMBRE = "X-User-Name";
    public static final String H_EMAIL = "X-User-Email";
    public static final String H_ROLES = "X-User-Roles";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(UsuarioActual.class)
                && UsuarioContexto.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
        String id = request.getHeader(H_ID);
        if (!StringUtils.hasText(id)) {
            throw new MissingRequestHeaderException(H_ID, parameter);
        }
        String nombre = decode(request.getHeader(H_NOMBRE));
        String email = decode(request.getHeader(H_EMAIL));
        String rolesHeader = request.getHeader(H_ROLES);
        Set<String> roles = StringUtils.hasText(rolesHeader)
                ? Arrays.stream(rolesHeader.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet())
                : Set.of();
        return new UsuarioContexto(id, StringUtils.hasText(nombre) ? nombre : email, email, roles);
    }

    private String decode(String value) {
        return value == null ? null : URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
