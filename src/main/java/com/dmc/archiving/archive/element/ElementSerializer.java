package com.dmc.archiving.archive.element;

import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.archive.element.link.ElementLink;
import org.springframework.modulith.NamedInterface;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes an Element tree to a JSON-compatible Map. Exposed to other modules
 * (sip, aip, dip) so each can serialize its own root element during export.
 */
@NamedInterface
public final class ElementSerializer {

    private ElementSerializer() {}

    public static Map<String, Object> serialize(Element element) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", element.getId());
        data.put("elementIdentifier", element.getElementIdentifier());
        data.put("entityName", element.getEntityName());
        data.put("entityType", element.getEntityType());
        data.put("title", element.getTitle());
        if (element.getDescription() != null) data.put("description", element.getDescription());
        if (element.getCreatedAt() != null) data.put("createdAt", element.getCreatedAt());
        if (element.getCreatedBy() != null) data.put("createdBy", element.getCreatedBy());

        if (element.getFields() != null && !element.getFields().isEmpty()) {
            Map<String, Object> fields = new LinkedHashMap<>();
            for (Field field : element.getFields()) {
                if (field.getValue() != null && !field.getValue().isEmpty()) {
                    fields.put(field.getName(), Map.of(
                            "label", field.getLabel() != null ? field.getLabel() : field.getName(),
                            "type", field.getType() != null ? field.getType() : "text",
                            "value", field.getValue()
                    ));
                }
            }
            if (!fields.isEmpty()) data.put("fields", fields);
        }

        if (element.getOutgoingLinks() != null && !element.getOutgoingLinks().isEmpty()) {
            List<Map<String, Object>> links = new ArrayList<>();
            for (ElementLink link : element.getOutgoingLinks()) {
                Map<String, Object> linkData = new LinkedHashMap<>();
                linkData.put("linkType", link.getLinkType());
                linkData.put("targetElementId", link.getTargetElement().getId());
                linkData.put("targetEntityName", link.getTargetElement().getEntityName());
                if (link.getLabel() != null) linkData.put("label", link.getLabel());
                links.add(linkData);
            }
            data.put("links", links);
        }

        if (element.getChildren() != null && !element.getChildren().isEmpty()) {
            List<Map<String, Object>> children = new ArrayList<>();
            for (Element child : element.getChildren()) {
                children.add(serialize(child));
            }
            data.put("children", children);
        }

        return data;
    }
}
