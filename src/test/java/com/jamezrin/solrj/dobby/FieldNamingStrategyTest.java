package com.jamezrin.solrj.dobby;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldNamingStrategyTest {

  @Nested
  class IdentityStrategyTests {
    @Test
    void returnsFieldNameAsIs() {
      assertEquals("myField", FieldNamingStrategy.IDENTITY.translateName("myField"));
    }

    @Test
    void returnsUpperCaseAsIs() {
      assertEquals("URL", FieldNamingStrategy.IDENTITY.translateName("URL"));
    }
  }

  @Nested
  class LowerUnderscoreStrategyTests {
    @Test
    void simplesCamelCase() {
      assertEquals("created_at", FieldNamingStrategy.LOWER_UNDERSCORE.translateName("createdAt"));
    }

    @Test
    void singleWord() {
      assertEquals("name", FieldNamingStrategy.LOWER_UNDERSCORE.translateName("name"));
    }

    @Test
    void alreadyLowerCase() {
      assertEquals("id", FieldNamingStrategy.LOWER_UNDERSCORE.translateName("id"));
    }

    @Test
    void acronymOnly() {
      assertEquals("url", FieldNamingStrategy.LOWER_UNDERSCORE.translateName("URL"));
    }

    @Test
    void acronymAtStart() {
      assertEquals("url_field", FieldNamingStrategy.LOWER_UNDERSCORE.translateName("URLField"));
    }

    @Test
    void acronymInMiddle() {
      assertEquals(
          "my_url_field", FieldNamingStrategy.LOWER_UNDERSCORE.translateName("myURLField"));
    }

    @Test
    void acronymAtEnd() {
      assertEquals("my_url", FieldNamingStrategy.LOWER_UNDERSCORE.translateName("myURL"));
    }

    @Test
    void consecutiveAcronym() {
      assertEquals(
          "https_request", FieldNamingStrategy.LOWER_UNDERSCORE.translateName("HTTPSRequest"));
    }

    @Test
    void multipleAcronyms() {
      // Adjacent all-uppercase acronyms merge because there is no case transition between them.
      assertEquals("my_httpurl", FieldNamingStrategy.LOWER_UNDERSCORE.translateName("myHTTPURL"));
    }

    @Test
    void emptyString() {
      assertEquals("", FieldNamingStrategy.LOWER_UNDERSCORE.translateName(""));
    }
  }

  @Nested
  class LowerCaseStrategyTests {
    @Test
    void convertsToLowerCase() {
      assertEquals("myfield", FieldNamingStrategy.LOWER_CASE.translateName("myField"));
    }

    @Test
    void acronymToLowerCase() {
      assertEquals("url", FieldNamingStrategy.LOWER_CASE.translateName("URL"));
    }
  }
}
