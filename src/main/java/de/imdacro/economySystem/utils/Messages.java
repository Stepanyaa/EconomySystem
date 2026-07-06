package de.imdacro.economySystem.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Messages {

    private final JsonObject messages;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private String prefix = ""; // Standardwert für Präfix

    public Messages(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            this.messages = JsonParser.parseReader(reader).getAsJsonObject();

            // Präfix aus der Datei lesen, falls vorhanden
            if (messages.has("prefix")) {
                this.prefix = messages.get("prefix").getAsString();
            }
        }
    }

    public Component get(String key, Object... placeholders) {
        if (!messages.has(key)) {
            return Component.text("Message not found: " + key);
        }

        String message = messages.get(key).getAsString();
        message = applyPlaceholders(message, placeholders);

        // Don't add prefix if the key IS the prefix or if message already contains prefix logic
        if (key.equals("prefix") || message.startsWith("<green>[EconomySystem]</green>")) {
            return miniMessage.deserialize(message);
        }

        // Präfix hinzufügen und Nachricht deserialisieren
        return miniMessage.deserialize(prefix + " " + message);
    }

    private String applyPlaceholders(String message, Object... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be in key-value pairs!");
        }

        for (int i = 0; i < placeholders.length; i += 2) {
            // Guard against null placeholders to avoid NPEs (e.g., offline players without a known name)
            Object keyObj = placeholders[i];
            Object valObj = placeholders[i + 1];
            String placeholder = keyObj == null ? "" : keyObj.toString();
            String value = valObj == null ? "" : valObj.toString();
            if (!placeholder.isEmpty()) {
                message = message.replace(placeholder, value);
            }
        }
        return message;
    }

    public String getPrefix() {
        return prefix;
    }

}
