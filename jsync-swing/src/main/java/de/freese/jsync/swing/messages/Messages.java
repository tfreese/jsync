// Created: 12.07.2020
package de.freese.jsync.swing.messages;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public class Messages
{
    /**
     *
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(Messages.class);

    /**
     *
     */
    private Map<String, String> messageMap = Collections.emptyMap();

    /**
     * Erstellt ein neues {@link Messages} Object.
     *
     * @param locale {@link Locale}
     */
    public Messages(final Locale locale)
    {
        super();

        setLocale(locale);
    }

    /**
     * @param key String
     *
     * @return String
     */
    public String getString(final String key)
    {
        String value = this.messageMap.get(key);

        if (value == null)
        {
            LOGGER.warn("no message for key: {}", key);

            value = "_" + key + "_";
        }

        return value;
    }

    /**
     * @param locale {@link Locale}
     */
    private void setLocale(final Locale locale)
    {
        Objects.requireNonNull(locale, "locale required");

        Map<String, String> map = new HashMap<>();

        ResourceBundle bundle = ResourceBundle.getBundle("bundles/jsync");

        Enumeration<String> keys = bundle.getKeys();

        while (keys.hasMoreElements())
        {
            String key = keys.nextElement();
            String value = bundle.getString(key);

            map.put(key, value);
        }

        this.messageMap = map;
    }
}
