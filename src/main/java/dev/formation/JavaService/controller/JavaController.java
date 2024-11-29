package dev.formation.JavaService.controller;

import dev.formation.JavaService.dto.CodeRequest;
import dev.formation.JavaService.dto.CodeResponse;
import dev.formation.JavaService.service.JavaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@CrossOrigin(origins = "*") // TODO a modifier
public class JavaController {
    private final JavaService javaService;

    public JavaController(JavaService javaService){
        this.javaService=javaService;
    }

    @PostMapping("/execute")
    public ResponseEntity<CodeResponse> compileAndExecute(@RequestBody CodeRequest codeRequest) {
        try {
            return ResponseEntity.ok(javaService.compileAndExecute(codeRequest.getCode()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new CodeResponse(e.getMessage(), null,null));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CodeResponse(e.getMessage(),null,null));
        }
    }
}
