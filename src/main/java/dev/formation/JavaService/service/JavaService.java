package dev.formation.JavaService.service;

import dev.formation.JavaService.dto.CodeResponse;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JavaService {

    private static final int TIMEOUT = 5; // Timeout en secondes

    public CodeResponse compileAndExecute(String code) {
        String className = extractClassName(code);
        if (className == null || className.isEmpty()) {
            throw new RuntimeException("Error: Could not find a public class declaration.");
        }

        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 5); // Générer un ID unique

        // Injecter l'ID unique dans le code source
        code = injectUniqueIdIntoClassName(code, className, uniqueId);
        String fullClassName = className + uniqueId;

        Path tempDir = null;
        try {
            // Créer un répertoire temporaire dédié
            tempDir = Files.createTempDirectory("java-compiler");
            Path sourceFile = tempDir.resolve(fullClassName + ".java");
            Path errorFile = tempDir.resolve("errors.log");
            Path outputFile = tempDir.resolve("output.log");

            // Écrire le code dans le fichier source
            Files.writeString(sourceFile, code, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Chronométrer la compilation
            long startTime = System.nanoTime();
            Process compileProcess = new ProcessBuilder("javac", sourceFile.toString())
                    .redirectError(errorFile.toFile())
                    .start();

            if (!compileProcess.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
                throw new RuntimeException("Compilation timed out.");
            }

            if (compileProcess.exitValue() != 0) {
                String errors = Files.readString(errorFile);
                throw new RuntimeException("Compilation failed\n\n" + formatCompilationError(errors));
            }

            // Chronométrer l'exécution
            Process executeProcess = new ProcessBuilder("java", "-cp", tempDir.toString(), fullClassName)
                    .redirectOutput(outputFile.toFile())
                    .redirectError(errorFile.toFile())
                    .start();

            if (!executeProcess.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
                throw new RuntimeException("Execution timed out.");
            }

            if (executeProcess.exitValue() != 0) {
                String errors = Files.readString(errorFile);
                throw new RuntimeException("Execution failed\n\n" + errors);
            }

            long executionTimeNs = System.nanoTime() - startTime;
            double executionTimeS = executionTimeNs / 1_000_000_000.0;

            // Lire et filtrer la sortie pour extraire MemoryUsage
            List<String> outputLines = Files.readAllLines(outputFile);
            StringBuilder filteredOutput = new StringBuilder();
            String memoryUsage = null;

            for (String line : outputLines) {
                if (line.startsWith("{MemoryUsage}:")) {
                    memoryUsage = line.split(":")[1].trim();
                } else {
                    filteredOutput.append(line).append("\n");
                }
            }

            return new CodeResponse(filteredOutput.toString().trim(), String.format("%.3f", executionTimeS), memoryUsage);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Unexpected error during execution: " + e.getMessage(), e);
        } finally {
            // Nettoyer les fichiers temporaires
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted((a, b) -> b.compareTo(a)) // Supprimer les fichiers avant les répertoires
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String extractClassName(String code) {
        String regex = "public\\s+class\\s+(\\w+)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String injectUniqueIdIntoClassName(String code, String className, String uniqueId) {
        return code.replaceFirst(
                "public\\s+class\\s+" + className,
                "public class " + className + uniqueId
        );
    }
    private String formatCompilationError(String rawError) {
        StringBuilder formattedError = new StringBuilder();
        String[] lines = rawError.split("\n");

        for (String line : lines) {
            if (line.contains(": error:")) {
                // Extraction du message d'erreur principal
                String[] parts = line.split(":");
                if (parts.length > 3) {
                    String fileName = parts[0].trim();
                    String lineNumber = parts[1].trim();
                    String errorMessage = parts[3].trim();

                    String trueLineNumber=String.valueOf(Integer.parseInt(lineNumber)-4);

                    formattedError.append("Error at line ").append(trueLineNumber)
                            .append(" : ").append(errorMessage)
                            .append("\n");
                }
            } else if (line.contains("^")) {
                // Ajouter une indication visuelle pour la position de l'erreur
//                formattedError.append("location: ").append(line).append("\n");
            } else if (line.contains("location")) {
                // Ajouter d'autres lignes utiles
//                formattedError.append(line).append("\n");
            }else if (!line.isBlank()) {
                // Ajouter d'autres lignes utiles
                formattedError.append(line).append("\n");
            }
        }

        if (formattedError.isEmpty()) {
            return rawError; // Si aucun formatage n'a été possible, renvoyer l'erreur brute
        }

        // Ajout de conseils génériques
//        formattedError.append("\nHint: Ensure each statement ends with a semicolon ';' and all parentheses/brackets are balanced.");

        return formattedError.toString();
    }

}

