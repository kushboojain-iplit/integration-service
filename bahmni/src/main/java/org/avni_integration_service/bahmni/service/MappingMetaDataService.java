package org.avni_integration_service.bahmni.service;

import org.avni_integration_service.bahmni.*;
import org.avni_integration_service.bahmni.repository.intmapping.MappingService;
import org.avni_integration_service.integration_data.domain.*;
import org.avni_integration_service.integration_data.repository.IgnoredIntegratingConceptRepository;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MappingMetaDataService {

    private final MappingService mappingService;

    private final MappingMetaDataRepository mappingMetaDataRepository;

    private final IgnoredIntegratingConceptRepository ignoredBahmniConceptRepository;

    private final BahmniMappingGroup bahmniMappingGroup;

    private final BahmniMappingType bahmniMappingType;

    private final MappingType[] bahmniEncounterMappingTypes;
    @Autowired
    public MappingMetaDataService(MappingService mappingService, MappingMetaDataRepository mappingMetaDataRepository,
                                  IgnoredIntegratingConceptRepository ignoredBahmniConceptRepository,
                                  BahmniMappingGroup bahmniMappingGroup, BahmniMappingType bahmniMappingType) {
        this.mappingService = mappingService;
        this.mappingMetaDataRepository = mappingMetaDataRepository;
        this.ignoredBahmniConceptRepository = ignoredBahmniConceptRepository;
        this.bahmniMappingGroup = bahmniMappingGroup;
        this.bahmniMappingType = bahmniMappingType;
        this.bahmniEncounterMappingTypes = new MappingType[]{bahmniMappingType.encounterType,
                bahmniMappingType.drugOrderEncounterType,
                bahmniMappingType.diagnosesEncounterType,
                bahmniMappingType.bahmniForm2Name,
                bahmniMappingType.labEncounterType};
    }


    public SubjectToPatientMetaData getForSubjectToPatient() {
        MappingMetaData patientIdentifierMapping = mappingService.findByMappingGroupAndMappingType(bahmniMappingGroup.patientSubject, bahmniMappingType.patientIdentifierConcept);
        String avniIdentifierConcept = patientIdentifierMapping.getAvniValue();
        MappingMetaDataCollection personAttributesMappingList = mappingService.findAll(bahmniMappingGroup.patientSubject, bahmniMappingType.personAttributeConcept);

        String encounterTypeUuid = mappingService.getBahmniValue(bahmniMappingGroup.patientSubject, bahmniMappingType.subjectEncounterType);

        String subjectUuidConceptUuid = mappingService.getBahmniValueForAvniIdConcept();

        return new SubjectToPatientMetaData(avniIdentifierConcept, encounterTypeUuid, subjectUuidConceptUuid, personAttributesMappingList);
    }

    public PatientToSubjectMetaData getForPatientToSubject() {
        String avniIdentifierConcept = mappingService.getAvniValue(bahmniMappingGroup.patientSubject, bahmniMappingType.patientIdentifierConcept);
        String patientEncounterType = Names.AvniPatientRegistrationEncounter;
        String patientIdentifierName = mappingService.getBahmniValue(bahmniMappingGroup.patientSubject, bahmniMappingType.patientIdentifierConcept);
        String bahmniEntityUuidConceptInAvni = mappingService.getAvniValue(bahmniMappingGroup.common, bahmniMappingType.bahmniUUIDConcept);
        MappingMetaDataCollection personAttributesMappingList = mappingService.findAll(bahmniMappingGroup.patientSubject, bahmniMappingType.personAttributeConcept);
        return new PatientToSubjectMetaData(bahmniEntityUuidConceptInAvni, avniIdentifierConcept, patientEncounterType, patientIdentifierName, personAttributesMappingList);
    }

    public BahmniEncounterToAvniEncounterMetaData getForBahmniEncounterToAvniEntities() {
        List<MappingMetaData> mappings = mappingMetaDataRepository.findAllByMappingType(bahmniMappingType.encounterType);
        BahmniEncounterToAvniEncounterMetaData metaData = new BahmniEncounterToAvniEncounterMetaData();
        metaData.addEncounterMappings(mappings);

        String bahmniEntityUuidConceptInAvni = mappingService.getAvniValue(bahmniMappingGroup.common, bahmniMappingType.bahmniUUIDConcept);
        metaData.setBahmniEntityUuidConcept(bahmniEntityUuidConceptInAvni);

        metaData.addLabMapping(mappingMetaDataRepository.findByMappingType(bahmniMappingType.labEncounterType));
        metaData.addDrugOrderMapping(mappingMetaDataRepository.findByMappingType(bahmniMappingType.drugOrderEncounterType));
        metaData.addDrugOrderConceptMapping(mappingMetaDataRepository.findByMappingType(bahmniMappingType.drugOrderConcept));
        metaData.setDiagnosesEncounterTypeMapping(mappingMetaDataRepository.findByMappingType(bahmniMappingType.diagnosesEncounterType));
        metaData.setDiagnosesConceptMapping(mappingMetaDataRepository.findByMappingType(bahmniMappingType.diagnosesConcept));
        metaData.addProgramMapping(mappingMetaDataRepository.findAllByMappingGroupAndMappingType(bahmniMappingGroup.programEnrolment, bahmniMappingType.bahmniFormCommunityProgram));
        metaData.setBahmniForm2Mappings(mappingMetaDataRepository.findAllByMappingType(bahmniMappingType.bahmniForm2Name));
        ArrayList<IgnoredIntegratingConcept> ignoredIntegratingConcepts = new ArrayList<>();
        ignoredBahmniConceptRepository.findAll().forEach(ignoredIntegratingConcepts::add);
        metaData.setIgnoredConcepts(ignoredIntegratingConcepts);
        return metaData;
    }

    public boolean isBahmniEncounterInAvni(String encounterType) {
        return mappingMetaDataRepository.findAllByMappingTypeInAndAvniValue(Arrays.asList(bahmniEncounterMappingTypes), encounterType).size() != 0;
    }

    public boolean isBahmniEncounterMappingDefined(String encounterType) {
        return mappingService.getBahmniFormUuidForGeneralEncounter(encounterType) != null;
    }
}
