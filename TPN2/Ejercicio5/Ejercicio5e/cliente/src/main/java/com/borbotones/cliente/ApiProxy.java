package com.borbotones.cliente;

import jakarta.servlet.http.*;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.multipart.*;

/** Backend del cliente: los tokens nunca se entregan al JavaScript ni se guardan en el navegador. */
@RestController
public class ApiProxy {
    private final OAuth2AuthorizedClientManager manager;
    private final RestClient api;
    private final String base;
    private final String issuer;
    public ApiProxy(OAuth2AuthorizedClientManager manager, RestClient.Builder builder,
            @Value("${app.api-url}") String base, @Value("${app.oauth.issuer}") String issuer) {
        this.manager=manager; this.api=builder.build(); this.base=base; this.issuer=issuer;
    }
    @GetMapping("/registro")
    ResponseEntity<Void> registro() { return ResponseEntity.status(302).location(URI.create(issuer+"/registro")).build(); }
    @GetMapping("/sesion")
    Map<String,Object> sesion(Authentication auth, CsrfToken csrf) {
        return Map.of("usuario",auth.getName(), "csrfToken",csrf.getToken(), "csrfHeader",csrf.getHeaderName(), "csrfParameter",csrf.getParameterName());
    }
    @RequestMapping(value="/api/**", method={RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT,RequestMethod.DELETE})
    ResponseEntity<byte[]> proxy(HttpServletRequest req, HttpServletResponse res, Authentication auth,
            @RequestBody(required=false) byte[] body) {
        try {
            var autorizado=manager.authorize(OAuth2AuthorizeRequest.withClientRegistrationId("local").principal(auth)
                    .attribute(HttpServletRequest.class.getName(),req).attribute(HttpServletResponse.class.getName(),res).build());
            if (autorizado==null) return error(401,"La sesion requiere un nuevo login");
            String query=req.getQueryString();
            URI destino=URI.create(base+req.getRequestURI()+(query==null ? "" : "?"+query));
            var llamada=api.method(HttpMethod.valueOf(req.getMethod())).uri(destino)
                    .headers(h -> h.setBearerAuth(autorizado.getAccessToken().getTokenValue()));
            MultipartHttpServletRequest multipart=org.springframework.web.util.WebUtils.getNativeRequest(req,MultipartHttpServletRequest.class);
            if (multipart!=null) {
                var partes=new LinkedMultiValueMap<String,Object>();
                for (var entry:multipart.getFileMap().entrySet()) {
                    MultipartFile file=entry.getValue();
                    var recurso=new ByteArrayResource(file.getBytes()) { @Override public String getFilename() { return "foto"; } };
                    partes.add(entry.getKey(),recurso);
                }
                llamada.contentType(MediaType.MULTIPART_FORM_DATA).body(partes);
            } else if (body!=null && body.length>0) {
                llamada.contentType(MediaType.parseMediaType(req.getContentType()==null ? "application/json" : req.getContentType())).body(body);
            }
            return llamada.exchange((request,response) -> {
                if (response.getStatusCode().value()==401) cerrarSesion(req);
                var headers=new HttpHeaders();
                if (response.getHeaders().getContentType()!=null) headers.setContentType(response.getHeaders().getContentType());
                headers.setCacheControl("no-store");
                return new ResponseEntity<>(response.getBody().readAllBytes(),headers,response.getStatusCode());
            });
        } catch (OAuth2AuthorizationException ex) { cerrarSesion(req); return error(401,"La sesion vencio. Vuelve a ingresar"); }
        catch (RestClientException | java.io.IOException ex) { return error(502,"No se pudo conectar con el servidor"); }
    }
    private void cerrarSesion(HttpServletRequest request) {
        var session=request.getSession(false);
        if (session!=null) session.invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    private ResponseEntity<byte[]> error(int status,String mensaje) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).cacheControl(CacheControl.noStore())
                .body(("{\"mensaje\":\""+mensaje+"\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
