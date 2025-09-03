package com.guesshole.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.io.IOException;
import java.util.*;

@Configuration
public class AssetVersioningConfig {

    @Bean
    public AssetVersioningDialect assetVersioningDialect() {
        return new AssetVersioningDialect();
    }

    public static class AssetVersioningDialect extends AbstractDialect implements IExpressionObjectDialect {
        private final AssetVersioningFactory expressionFactory = new AssetVersioningFactory();

        public AssetVersioningDialect() {
            super("assetVersioning");
        }

        @Override
        public IExpressionObjectFactory getExpressionObjectFactory() {
            return expressionFactory;
        }
    }

    public static class AssetVersioningFactory implements IExpressionObjectFactory {
        private static final String ASSET_HELPER_NAME = "asset";
        private static final Set<String> EXPRESSION_OBJECT_NAMES = Collections.singleton(ASSET_HELPER_NAME);
        private Map<String, String> assetManifest = null;

        @Override
        public Set<String> getAllExpressionObjectNames() {
            return EXPRESSION_OBJECT_NAMES;
        }

        @Override
        public Object buildObject(IExpressionContext ctx, String expressionObjectName) {
            if (ASSET_HELPER_NAME.equals(expressionObjectName)) {
                return new AssetHelper(getAssetManifest());
            }
            return null;
        }

        @Override
        public boolean isCacheable(String expressionObjectName) {
            return true;
        }

        @SuppressWarnings("unchecked")
        private synchronized Map<String, String> getAssetManifest() {
            if (assetManifest == null) {
                assetManifest = new HashMap<>();
                Resource resource = new ClassPathResource("static/.vite/manifest.json");

                if (resource.exists()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Map<String, Object>> rawManifest = mapper.readValue(resource.getInputStream(),
                                new TypeReference<Map<String, Map<String, Object>>>() {});

                        // Process the new manifest structure
                        for (Map.Entry<String, Map<String, Object>> entry : rawManifest.entrySet()) {
                            Map<String, Object> details = entry.getValue();

                            // Handle JS entries
                            if (details.containsKey("name") && details.containsKey("file")) {
                                String name = (String) details.get("name");
                                String file = (String) details.get("file");
                                assetManifest.put(name, "/" + file);

                                // Check if this entry has associated CSS files
                                if (details.containsKey("css") && details.get("css") instanceof List) {
                                    List<String> cssFiles = (List<String>) details.get("css");
                                    for (String cssFile : cssFiles) {
                                        // Create an additional entry for each CSS file with the name-css key pattern
                                        assetManifest.put(name + "-css", "/" + cssFile);
                                    }
                                }
                            }

                            // Handle standalone CSS entries (those without a name)
                            else if (details.containsKey("file") && ((String) details.get("file")).endsWith(".css")) {
                                String file = (String) details.get("file");
                                String src = (String) details.get("src");

                                // Extract file name without extension
                                String cssName = src.substring(src.lastIndexOf('/') + 1);
                                cssName = cssName.substring(0, cssName.lastIndexOf('.'));

                                // If the source is input.css, use "main" as the name
                                if (cssName.equals("input")) {
                                    cssName = "main";
                                }

                                assetManifest.put(cssName, "/" + file);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to load asset manifest: " + e.getMessage());
                    }
                }
            }
            return assetManifest;
        }
    }

    public static class AssetHelper {
        private final Map<String, String> assetManifest;

        public AssetHelper(Map<String, String> assetManifest) {
            this.assetManifest = assetManifest;
        }

        public String path(String originalPath) {
            return assetManifest.getOrDefault(originalPath, originalPath);
        }
    }
}