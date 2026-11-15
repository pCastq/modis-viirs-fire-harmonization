package org.firecal.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** I due cataloghi dei messaggi devono dire le stesse cose: stesse chiavi, stessi segnaposto. */
class ApiMessagesTest {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");

  private static ResourceBundle bundle(Locale locale) {
    // Control.getNoFallbackControl: senza ripiego sulla lingua del server, come in ApiConfig
    return ResourceBundle.getBundle("messages/api", locale, ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
  }

  private static Set<String> placeholders(String text) {
    Set<String> found = new TreeSet<>();
    Matcher m = PLACEHOLDER.matcher(text);
    while (m.find()) {
      found.add(m.group(1));
    }
    return found;
  }

  @Test
  void bothLanguagesDefineTheSameKeys() {
    Set<String> english = new TreeSet<>(Collections.list(bundle(Locale.ENGLISH).getKeys()));
    Set<String> italian = new TreeSet<>(Collections.list(bundle(Locale.ITALIAN).getKeys()));
    assertThat(italian).containsExactlyInAnyOrderElementsOf(english);
    assertThat(english).hasSizeGreaterThan(30);
  }

  @Test
  void bothLanguagesUseTheSamePlaceholders() {
    ResourceBundle en = bundle(Locale.ENGLISH);
    ResourceBundle it = bundle(Locale.ITALIAN);
    for (String key : Collections.list(en.getKeys())) {
      assertThat(placeholders(it.getString(key))).as(key).isEqualTo(placeholders(en.getString(key)));
    }
  }

  @Test
  void noMessageIsEmptyAndEveryOneFormatsWithoutErrors() {
    List<Locale> languages = List.of(Locale.ENGLISH, Locale.ITALIAN);
    for (Locale locale : languages) {
      ResourceBundle bundle = bundle(locale);
      for (String key : Collections.list(bundle.getKeys())) {
        String text = bundle.getString(key);
        assertThat(text.strip()).as(key).isNotEmpty();
        Set<String> used = new HashSet<>(placeholders(text));
        Object[] args = new Object[used.size()];
        java.util.Arrays.fill(args, "x");
        // un apostrofo dritto non raddoppiato inghiottirebbe parte della frase: deve restare tutto
        String formatted = new MessageFormat(text, locale).format(args);
        assertThat(formatted).as(key).doesNotContain("{").hasSizeGreaterThanOrEqualTo(text.length() - used.size() * 3);
      }
    }
  }
}
