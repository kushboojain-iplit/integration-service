package org.avni_integration_service.bahmni.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenMRSSavePatientIdentifier {
    private String identifier;
    private String identifierType;
    private boolean preferred;

    public OpenMRSSavePatientIdentifier() {
    }

    public OpenMRSSavePatientIdentifier(String identifier, String identifierType, boolean preferred) {
        this.identifier = identifier;
        this.identifierType = identifierType;
        this.preferred = preferred;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public void setPreferred(boolean preferred) {
        this.preferred = preferred;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

}
