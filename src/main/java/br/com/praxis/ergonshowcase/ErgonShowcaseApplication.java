package br.com.praxis.ergonshowcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Host de demonstração do cadastro civil e funcional da administração pública. */
@SpringBootApplication(
    scanBasePackages = {"br.com.praxis.ergonshowcase", "org.praxisplatform.uischema",
        "org.praxisplatform.config"},
    excludeName = {
        "org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration",
        "org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration",
        "org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiTextEmbeddingAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration"
    })
@io.swagger.v3.oas.annotations.OpenAPIDefinition(
    info = @io.swagger.v3.oas.annotations.info.Info(
        title = "Ergon Showcase — Administração de Pessoal", version = "0.1.0",
        description = "Cadastro institucional, civil e funcional com operações administrativas governadas."),
    servers = @io.swagger.v3.oas.annotations.servers.Server(url = "/"))
public class ErgonShowcaseApplication {
  public static void main(String[] args) {
    SpringApplication.run(ErgonShowcaseApplication.class, args);
  }
}
