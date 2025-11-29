package org.dreamcat.daily.script.model;

import lombok.Data;

import java.util.Set;

/**
 * @author Jerry Will
 * @version 2023-05-29
 */
@Data
public class ConverterItem {

    private String template;
    private Set<String> types;
}
