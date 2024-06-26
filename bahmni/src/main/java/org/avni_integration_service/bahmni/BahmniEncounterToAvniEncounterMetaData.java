package org.avni_integration_service.bahmni;

import org.avni_integration_service.integration_data.domain.IgnoredIntegratingConcept;
import org.avni_integration_service.integration_data.domain.MappingMetaData;

import java.util.List;

public class BahmniEncounterToAvniEncounterMetaData implements BahmniToAvniMetaData {
    private String bahmniEntityUuidConcept;
    private List<MappingMetaData> encounterTypeMappings;
    private MappingMetaData labEncounterTypeMapping;
    private MappingMetaData drugOrderEncounterTypeMapping;
    private MappingMetaData drugOrderConceptMapping;

    private MappingMetaData diagnosesEncounterTypeMapping;
    private MappingMetaData diagnosesConceptMapping;
    private List<MappingMetaData> programMappings;
    private List<IgnoredIntegratingConcept> ignoredIntegratingConcepts;

    private List<MappingMetaData> bahmniForm2Mappings;

    public String getAvniMappedName(String openmrsEncounterTypeUuid) {
        MappingMetaData mapping = getMappingMetaData(openmrsEncounterTypeUuid);
        if (mapping != null) return mapping.getAvniValue();
        return null;
    }

    private MappingMetaData getMappingMetaData(String openmrsEncounterTypeUuid) {
        return encounterTypeMappings.stream().filter(mappingMetaData -> mappingMetaData.getIntSystemValue().equals(openmrsEncounterTypeUuid)).findFirst().orElse(null);
    }

    public boolean hasBahmniConceptSet(String openmrsEncounterTypeUuid) {
        return getMappingMetaData(openmrsEncounterTypeUuid) != null;
    }

    public void setBahmniEntityUuidConcept(String bahmniEntityUuidConcept) {
        this.bahmniEntityUuidConcept = bahmniEntityUuidConcept;
    }

    public String getBahmniEntityUuidConcept() {
        return bahmniEntityUuidConcept;
    }

    public void addEncounterMappings(List<MappingMetaData> encounterTypeMappings) {
        this.encounterTypeMappings = encounterTypeMappings;
    }

    public MappingMetaData getEncounterMappingFor(String openMRSEncounterUuid) {
        return encounterTypeMappings.stream().filter(x -> x.getIntSystemValue().equals(openMRSEncounterUuid)).findFirst().orElse(null);
    }

    public void addLabMapping(MappingMetaData labMapping) {
        this.labEncounterTypeMapping = labMapping;
    }

    public MappingMetaData getLabEncounterTypeMapping() {
        return labEncounterTypeMapping;
    }

    public void addDrugOrderMapping(MappingMetaData drugOrderMapping) {
        this.drugOrderEncounterTypeMapping = drugOrderMapping;
    }

    public MappingMetaData getDrugOrderEncounterTypeMapping() {
        return drugOrderEncounterTypeMapping;
    }

    public void addDrugOrderConceptMapping(MappingMetaData drugOrderConceptMapping) {
        this.drugOrderConceptMapping = drugOrderConceptMapping;
    }

    public MappingMetaData getDrugOrderConceptMapping() {
        return drugOrderConceptMapping;
    }

    public void addProgramMapping(List<MappingMetaData> mappings) {
        this.programMappings = mappings;
    }

    public String getAvniProgramName(String formConceptSetUuid) {
        MappingMetaData programMapping = this.programMappings.stream().filter(mappingMetaData -> mappingMetaData.getIntSystemValue().equals(formConceptSetUuid)).findFirst().orElse(null);
        if (programMapping == null) return null;
        return programMapping.getAvniValue();
    }

    public void setIgnoredConcepts(List<IgnoredIntegratingConcept> ignoredIntegratingConcepts) {
        this.ignoredIntegratingConcepts = ignoredIntegratingConcepts;
    }

    public boolean isIgnoredInBahmni(String conceptUuid) {
        return ignoredIntegratingConcepts.stream().filter(ignoredIntegratingConcept -> ignoredIntegratingConcept.getConceptId().equals(conceptUuid)).findFirst().orElse(null) != null;
    }

    public MappingMetaData getDiagnosesEncounterTypeMapping() {
        return diagnosesEncounterTypeMapping;
    }

    public MappingMetaData getDiagnosesConceptMapping() {
        return diagnosesConceptMapping;
    }

    public void setDiagnosesEncounterTypeMapping(MappingMetaData diagnosesEncounterTypeMapping) {
        this.diagnosesEncounterTypeMapping = diagnosesEncounterTypeMapping;
    }

    public void setDiagnosesConceptMapping(MappingMetaData diagnosesConceptMapping) {
        this.diagnosesConceptMapping = diagnosesConceptMapping;
    }

    public boolean hasDiagnosesMappings(){
        return this.diagnosesConceptMapping != null && this.diagnosesEncounterTypeMapping != null;
    }

    public void setBahmniForm2Mappings(List<MappingMetaData> bahmniForm2Mappings) {
        this.bahmniForm2Mappings = bahmniForm2Mappings;
    }

    public boolean hasBahmniForm2Mappings(){
        return this.bahmniForm2Mappings != null && !this.bahmniForm2Mappings.isEmpty();
    }

    public MappingMetaData getBahmniForm2MappingFor(String bahmniForm2Name) {
        return bahmniForm2Mappings.stream().filter(bahmniForm2Mapping -> bahmniForm2Mapping.getIntSystemValue().equals(bahmniForm2Name)).findFirst().orElse(null);
    }
}
