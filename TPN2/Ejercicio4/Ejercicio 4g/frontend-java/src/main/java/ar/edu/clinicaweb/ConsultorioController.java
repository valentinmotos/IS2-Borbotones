package ar.edu.clinicaweb;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.io.IOException;

/** Frontend Java: renderiza Thymeleaf y consume exclusivamente la API HTTP. */
@Controller
public class ConsultorioController {
    private final String api;
    private final ObjectMapper json;
    private static final Set<String> RECURSOS=Set.of("pacientes","medicos","usuarios","historias");
    public ConsultorioController(@Value("${clinica.api}") String api,ObjectMapper json) { this.api=api; this.json=json; }
    private RestClient cliente(HttpSession s) {
        return RestClient.builder().baseUrl(api).defaultHeaders(h->h.setBasicAuth((String)s.getAttribute("usuario"),(String)s.getAttribute("clave"),java.nio.charset.StandardCharsets.UTF_8)).build();
    }
    @GetMapping("/login") String login() { return "login"; }
    @PostMapping("/login") String entrar(@RequestParam String usuario,@RequestParam String clave,HttpSession s,RedirectAttributes flash) {
        s.setAttribute("usuario",usuario); s.setAttribute("clave",clave);
        try { cliente(s).get().uri("/usuarios").retrieve().toBodilessEntity(); }
        catch(RestClientException ex) { s.removeAttribute("usuario"); s.removeAttribute("clave"); flash.addFlashAttribute("error","No se pudo ingresar. Verificá las credenciales y que el backend esté iniciado."); return "redirect:/login"; }
        return "redirect:/";
    }
    @PostMapping("/salir") String salir(HttpSession s) { s.invalidate(); return "redirect:/login"; }
    @GetMapping("/") String panel(@RequestParam(defaultValue="historias") String vista,@RequestParam(required=false) String editar,@RequestParam(required=false) String historia,@RequestParam(required=false) String detalleEditar,Model model,HttpSession s) {
        if (!RECURSOS.contains(vista)) vista="historias";
        var c=cliente(s);
        for (String r:RECURSOS) model.addAttribute(r,c.get().uri("/"+r).retrieve().body(List.class));
        model.addAttribute("vista",vista);
        Object ed=Map.of();
        if(editar!=null) {
            Map<?,?> respuesta=c.get().uri("/"+vista+"/"+editar).retrieve().body(Map.class);
            ed=vista.equals("historias")?respuesta.get("historia"):respuesta;
        }
        model.addAttribute("ed",ed);
        model.addAttribute("expediente",historia==null?null:c.get().uri("/historias/"+historia).retrieve().body(Map.class));
        model.addAttribute("atencion",detalleEditar==null||historia==null?Map.of():c.get().uri("/historias/"+historia+"/detalles/"+detalleEditar).retrieve().body(Map.class));
        return "panel";
    }
    @PostMapping("/guardar/{recurso}") String guardar(@PathVariable String recurso,@RequestParam Map<String,String> form,HttpSession s,RedirectAttributes flash) {
        if(!RECURSOS.contains(recurso)) throw new IllegalArgumentException("Recurso inválido");
        Map<String,Object> datos=new HashMap<>();
        List<String> campos=switch(recurso) { case "usuarios"->List.of("nombreUsuario","clave"); case "historias"->List.of("pacienteId","usuarioId"); default->List.of("nombre","apellido","documento"); };
        for(String campo:campos) datos.put(campo,form.get(campo));
        if(recurso.equals("usuarios") && form.getOrDefault("clave","").isBlank()) datos.put("clave",null);
        String id=form.getOrDefault("id","");
        cliente(s).method(id.isBlank()?HttpMethod.POST:HttpMethod.PUT).uri("/"+recurso+(id.isBlank()?"":"/"+id)).body(datos).retrieve().toBodilessEntity();
        flash.addFlashAttribute("exito","Registro guardado"); return "redirect:/?vista="+recurso;
    }
    @PostMapping("/eliminar/{recurso}/{id}") String eliminar(@PathVariable String recurso,@PathVariable String id,HttpSession s,RedirectAttributes flash) {
        if(!RECURSOS.contains(recurso)) throw new IllegalArgumentException("Recurso inválido");
        cliente(s).delete().uri("/"+recurso+"/"+id).retrieve().toBodilessEntity();
        flash.addFlashAttribute("exito","Se realizó la baja lógica"); return "redirect:/?vista="+recurso;
    }
    @PostMapping("/historias/{id}/detalles") String guardarDetalle(@PathVariable String id,@RequestParam Map<String,String> form,HttpSession s,RedirectAttributes flash) {
        String detalleId=form.getOrDefault("detalleId","");
        Map<String,String> datos=new HashMap<>();
        for(String campo:List.of("fechaHistoria","detalleHistoria","medicoId","tipo")) datos.put(campo,form.get(campo));
        cliente(s).method(detalleId.isBlank()?HttpMethod.POST:HttpMethod.PUT).uri("/historias/"+id+"/detalles"+(detalleId.isBlank()?"":"/"+detalleId)).body(datos).retrieve().toBodilessEntity();
        flash.addFlashAttribute("exito","Atención guardada"); return "redirect:/?historia="+id;
    }
    @PostMapping("/historias/{id}/detalles/{detalleId}/eliminar") String eliminarDetalle(@PathVariable String id,@PathVariable String detalleId,HttpSession s,RedirectAttributes flash) {
        cliente(s).delete().uri("/historias/"+id+"/detalles/"+detalleId).retrieve().toBodilessEntity();
        flash.addFlashAttribute("exito","Atención eliminada"); return "redirect:/?historia="+id;
    }
    @PostMapping("/pacientes/{id}/foto") String foto(@PathVariable String id,@RequestParam MultipartFile archivo,HttpSession s,RedirectAttributes flash) throws IOException {
        var partes=new LinkedMultiValueMap<String,Object>();
        var headers=new HttpHeaders(); headers.setContentType(MediaType.parseMediaType(Objects.requireNonNullElse(archivo.getContentType(),"application/octet-stream")));
        partes.add("archivo",new HttpEntity<>(new ByteArrayResource(archivo.getBytes()) { @Override public String getFilename() { return archivo.getOriginalFilename(); } },headers));
        cliente(s).post().uri("/pacientes/"+id+"/foto").contentType(MediaType.MULTIPART_FORM_DATA).body(partes).retrieve().toBodilessEntity();
        flash.addFlashAttribute("exito","Foto actualizada"); return "redirect:/?vista=pacientes";
    }
    @PostMapping("/pacientes/{id}/foto/eliminar") String eliminarFoto(@PathVariable String id,HttpSession s) {
        cliente(s).delete().uri("/pacientes/"+id+"/foto").retrieve().toBodilessEntity(); return "redirect:/?vista=pacientes";
    }
    @GetMapping("/pacientes/{id}/foto") @ResponseBody ResponseEntity<byte[]> foto(@PathVariable String id,HttpSession s) {
        return cliente(s).get().uri("/pacientes/"+id+"/foto").retrieve().toEntity(byte[].class);
    }
    @ExceptionHandler(RestClientException.class) String apiError(RestClientException e,Model model) {
        String mensaje="No se pudo conectar con el backend";
        if(e instanceof RestClientResponseException r) {
            mensaje=r.getStatusCode().value()==401?"La sesión dejó de ser válida. Volvé a ingresar.":"La API rechazó la operación";
            try { mensaje=json.readTree(r.getResponseBodyAsString()).path("mensaje").asText(mensaje); } catch(Exception ignored) { }
        }
        model.addAttribute("error",mensaje); return "fallo";
    }
}
