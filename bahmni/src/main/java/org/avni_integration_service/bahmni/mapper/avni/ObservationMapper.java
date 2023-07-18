package org.avni_integration_service.bahmni.mapper.avni;

import org.avni_integration_service.bahmni.BahmniMappingGroup;
import org.avni_integration_service.bahmni.BahmniMappingType;
import org.avni_integration_service.bahmni.MappingMetaDataCollection;
import org.avni_integration_service.bahmni.contract.OpenMRSObservation;
import org.avni_integration_service.bahmni.contract.OpenMRSSaveObservation;
import org.avni_integration_service.bahmni.repository.intmapping.MappingService;
import org.avni_integration_service.integration_data.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.avni_integration_service.bahmni.contract.OpenMRSSaveObservation.createVoidedObs;

@Component
public class ObservationMapper {
    private final MappingService mappingService;
    private final BahmniMappingGroup bahmniMappingGroup;

    private final BahmniMappingType bahmniMappingType;

    @Value("${bahmni.mapping.complex.concepts}")
    private String complexConcepts;

    @Autowired
    public ObservationMapper(MappingService mappingService, BahmniMappingGroup bahmniMappingGroup, BahmniMappingType bahmniMappingType) {
        this.mappingService = mappingService;
        this.bahmniMappingGroup = bahmniMappingGroup;
        this.bahmniMappingType = bahmniMappingType;
    }

    public List<OpenMRSSaveObservation> updateOpenMRSObservationsFromAvniObservations(List<OpenMRSObservation> openMRSObservations, Map<String, Object> avniObservations, List<String> hardcodedConcepts) {
        List<OpenMRSSaveObservation> updateObservations = new ArrayList<>();
        MappingMetaDataCollection conceptMappings = mappingService.findAll(bahmniMappingGroup.observation, bahmniMappingType.concept);
        updateObservations.addAll(voidedObservations(openMRSObservations, avniObservations, conceptMappings, hardcodedConcepts));
        updateObservations.addAll(updatedObservations(openMRSObservations, avniObservations, conceptMappings));
        updateObservations.addAll(hardcodedObservations(openMRSObservations, hardcodedConcepts));
        return updateObservations;
    }

    private List<OpenMRSSaveObservation> hardcodedObservations(List<OpenMRSObservation> openMRSObservations, List<String> hardcodedConcepts) {
        List<OpenMRSSaveObservation> updateObservations = new ArrayList<>();
        for (var hardcodedConcept : hardcodedConcepts) {
            Optional<OpenMRSObservation> hardCodedObs = openMRSObservations.stream().filter(o -> o.getConceptUuid().equals(hardcodedConcept)).findFirst();
            hardCodedObs.ifPresent(existing -> {
                OpenMRSSaveObservation openMRSSaveObservation = new OpenMRSSaveObservation();
                openMRSSaveObservation.setUuid(existing.getObsUuid());
                openMRSSaveObservation.setConcept(existing.getConceptUuid());
                openMRSSaveObservation.setValue(existing.getValue());
                updateObservations.add(openMRSSaveObservation);
            });
        }
        return updateObservations;
    }

    private List<OpenMRSSaveObservation> updatedObservations(List<OpenMRSObservation> openMRSObservations, Map<String, Object> avniObservations, MappingMetaDataCollection conceptMappings) {
        List<OpenMRSSaveObservation> updatedObservations = new ArrayList<>();
        for (Map.Entry<String, Object> entry : avniObservations.entrySet()) {
            String question = entry.getKey();
            Object answer = entry.getValue();
            MappingMetaData questionMapping = conceptMappings.getMappingForAvniValue(question);
            if (questionMapping != null) {
                if (questionMapping.isCoded()) {
                    if (answer instanceof String) {
                        var avniAnswer = (String) answer;
                        if (isComplexConcept(questionMapping.getIntSystemValue())) {
                            updatedObservations.add(updatedComplexObs(openMRSObservations, conceptMappings, question, questionMapping, avniAnswer));
                        } else {
                            updatedObservations.add(updatedCodedObs(openMRSObservations, conceptMappings, question, questionMapping, avniAnswer));
                        }
                    } else if (answer instanceof List<?>) {
                        List<String> valueList = (List<String>) answer;
                        valueList.forEach(avniAnswer -> {
                            if (isComplexConcept(questionMapping.getIntSystemValue())) {
                                updatedObservations.add(updatedComplexObs(openMRSObservations, conceptMappings, question, questionMapping, avniAnswer));
                            } else {
                                updatedObservations.add(updatedCodedObs(openMRSObservations, conceptMappings, question, questionMapping, avniAnswer));
                            }
                        });
                    }
                                        } else {
                    if (questionMapping.isText() && answer instanceof String && ((String) answer).isBlank()) {
                        continue;
                    }
                    updatedObservations.add(updatedPrimitiveObs(openMRSObservations, conceptMappings, question, questionMapping, answer));
                }
            }
        }
        return updatedObservations;

    }

    private OpenMRSSaveObservation updatedPrimitiveObs(List<OpenMRSObservation> openMRSObservations, MappingMetaDataCollection conceptMappings, String question, MappingMetaData questionMapping, Object answer) {
        OpenMRSObservation openMRSObservation = openMRSObservations.stream()
                .filter(o -> o.getConceptUuid().equals(conceptMappings.getBahmniValueForAvniValue(question)))
                .findFirst()
                .orElse(null);
        if (openMRSObservation != null) {
            return (OpenMRSSaveObservation.createPrimitiveObs(openMRSObservation.getObsUuid(), openMRSObservation.getConceptUuid(), answer, questionMapping.getDataTypeHint()));
        } else {
            return (OpenMRSSaveObservation.createPrimitiveObs(questionMapping.getIntSystemValue(), answer, questionMapping.getDataTypeHint()));
        }
    }

    private OpenMRSSaveObservation updatedCodedObs(List<OpenMRSObservation> openMRSObservations, MappingMetaDataCollection conceptMappings, String question, MappingMetaData questionMapping, String avniAnswerConcept) {
        MappingMetaData answerMapping = conceptMappings.getMappingForAvniValue(avniAnswerConcept);
        OpenMRSObservation openMRSObservation = openMRSObservations.stream()
                .filter(o -> o.getConceptUuid().equals(conceptMappings.getBahmniValueForAvniValue(question)) &&
                        o.getValue().equals(conceptMappings.getBahmniValueForAvniValue(avniAnswerConcept)))
                .findFirst()
                .orElse(null);
        if (openMRSObservation != null) {
            return (OpenMRSSaveObservation.createCodedObs(openMRSObservation.getObsUuid(), questionMapping.getIntSystemValue(), answerMapping.getIntSystemValue()));
        } else {
            return (OpenMRSSaveObservation.createCodedObs(questionMapping.getIntSystemValue(), answerMapping.getIntSystemValue()));
        }
    }

    private OpenMRSSaveObservation updatedComplexObs(List<OpenMRSObservation> openMRSObservations, MappingMetaDataCollection conceptMappings, String question, MappingMetaData questionMapping, String avniAnswerConcept) {
        MappingMetaData answerMapping = conceptMappings.getMappingForAvniValue(avniAnswerConcept);
        OpenMRSObservation openMRSObservation = openMRSObservations.stream()
                .filter(o -> o.getConceptUuid().equals(conceptMappings.getBahmniValueForAvniValue(question)) &&
                        o.getValueComplex().equals(conceptMappings.getBahmniValueForAvniValue(avniAnswerConcept)))
                .findFirst()
                .orElse(null);
        if (openMRSObservation != null) {
            return (OpenMRSSaveObservation.createComplexObs(openMRSObservation.getObsUuid(), questionMapping.getIntSystemValue(), answerMapping.getIntSystemValue()));
        } else {
            return (OpenMRSSaveObservation.createComplexObs(questionMapping.getIntSystemValue(), answerMapping.getIntSystemValue()));
        }
    }

    private List<OpenMRSSaveObservation> voidedObservations(List<OpenMRSObservation> openMRSObservations, Map<String, Object> avniObservations, MappingMetaDataCollection conceptMappings, List<String> exclude) {
        List<OpenMRSSaveObservation> voidedObservations = new ArrayList<>();
        openMRSObservations.stream().filter(o -> !exclude.contains(o.getConceptUuid())).forEach(openMRSObservation -> {
            MappingMetaData questionMapping = conceptMappings.getMappingForBahmniValue(openMRSObservation.getConceptUuid());
            String avniConceptName = questionMapping.getAvniValue();
            Object avniObsValue = avniObservations.get(avniConceptName);

            if (avniObsValue == null) {
                voidedObservations.add(createVoidedObs(openMRSObservation.getObsUuid(), openMRSObservation.getConceptUuid()));
            } else if (questionMapping.isCoded()) {
                String openMRSAnswerName = isComplexConcept(openMRSObservation.getConceptUuid()) ? conceptMappings.getAvniValueForBahmniValue((String) openMRSObservation.getValueComplex()) : conceptMappings.getAvniValueForBahmniValue((String) openMRSObservation.getValue());
                if (avniObsValue instanceof List<?>) {
                    List<String> avniObsValueList = (List<String>) avniObsValue;
                    if (!avniObsValueList.contains(openMRSAnswerName)) {
                        voidedObservations.add(createVoidedObs(openMRSObservation.getObsUuid(), openMRSObservation.getConceptUuid()));
                    }
                } else if (avniObsValue instanceof String) {
                    String avniObsValueString = (String) avniObsValue;
                    if (!avniObsValueString.equals(openMRSAnswerName)) {
                        voidedObservations.add(createVoidedObs(openMRSObservation.getObsUuid(), openMRSObservation.getConceptUuid()));
                    }
                }
            }
        });
        return voidedObservations;
    }

    public List<OpenMRSSaveObservation> mapObservations(Map<String, Object> avniObservations) {
        List<OpenMRSSaveObservation> openMRSObservations = new ArrayList<>();
        MappingMetaDataCollection conceptMappings = mappingService.findAll(bahmniMappingGroup.observation, bahmniMappingType.concept);
        for (Map.Entry<String, Object> entry : avniObservations.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            MappingMetaData questionMapping = conceptMappings.getMappingForAvniValue(key);
            if (questionMapping != null) {
                if (questionMapping.isCoded()) {
                    if (value instanceof String) {
                        MappingMetaData answerMapping = conceptMappings.getMappingForAvniValue((String) value);
                        if (isComplexConcept(questionMapping.getIntSystemValue())) {
                            openMRSObservations.add(OpenMRSSaveObservation.createComplexObs(questionMapping.getIntSystemValue(), answerMapping.getIntSystemValue()));
                        } else {
                            openMRSObservations.add(OpenMRSSaveObservation.createCodedObs(questionMapping.getIntSystemValue(), answerMapping.getIntSystemValue()));
                        }
                    }
                    else if (value instanceof List<?>) {
                        List<String> valueList = (List<String>) value;
                        valueList.forEach(s -> {
                            MappingMetaData answerMapping = conceptMappings.getMappingForAvniValue(s);
                            if (isComplexConcept(questionMapping.getIntSystemValue())) {
                                openMRSObservations.add(OpenMRSSaveObservation.createComplexObs(questionMapping.getIntSystemValue(), answerMapping.getIntSystemValue()));
                            } else {
                                openMRSObservations.add(OpenMRSSaveObservation.createCodedObs(questionMapping.getIntSystemValue(), answerMapping.getIntSystemValue()));
                            }
                        });
                    }
                } else {
                    if (value instanceof List<?>) {
                        List<?> valueList = (List<Map>) value;
                        valueList.forEach(element -> {
                            if (element instanceof Map<?, ?>) {
                                Map<?, ?> questionGroupMap = (Map<String, Object>) element;
                                List<OpenMRSSaveObservation> groupMembers = new ArrayList<>();
                                List<OpenMRSSaveObservation> newGroupMembers = new ArrayList<>();
                                boolean isUniqueGroup = true;
                                for (Map.Entry<?, ?> e : questionGroupMap.entrySet()) {
                                    Object key1 = e.getKey();
                                    Object value1 = e.getValue();
                                    if (key1 instanceof String) {
                                        String keyString = (String) key1;
                                        MappingMetaData answerMapping = conceptMappings.getMappingForAvniValue(keyString);
                                        boolean isUnique = true;
                                        for (OpenMRSSaveObservation grpMember : groupMembers) {
                                            if (grpMember.getConcept().equals(answerMapping.getIntSystemValue())) {
                                                isUnique = false;
                                                isUniqueGroup = false;
                                                break;
                                            }
                                        }
                                        if (isUnique) {
                                            groupMembers.add(OpenMRSSaveObservation.createPrimitiveObs(answerMapping.getIntSystemValue(), value1, questionMapping.getDataTypeHint()));
                                        } else {
                                            newGroupMembers.add(OpenMRSSaveObservation.createPrimitiveObs(answerMapping.getIntSystemValue(), value1, questionMapping.getDataTypeHint()));
                                        }
                                    }
                                }
                                if (!groupMembers.isEmpty()) {
                                    openMRSObservations.add(OpenMRSSaveObservation.createPrimitiveObsForQuestionGroup(questionMapping.getIntSystemValue(), groupMembers));
                                }
                                if (!newGroupMembers.isEmpty() && !isUniqueGroup) {
                                    openMRSObservations.add(OpenMRSSaveObservation.createPrimitiveObsForQuestionGroup(questionMapping.getIntSystemValue(), newGroupMembers));
                                }
                            }
                        });
                        continue;
                    }
                    if (questionMapping.isText() && value instanceof String && ((String) value).isBlank()) {
                        continue;
                    }
                    openMRSObservations.add(OpenMRSSaveObservation.createPrimitiveObs(questionMapping.getIntSystemValue(), value, questionMapping.getDataTypeHint()));
                }
            }
        }
        return openMRSObservations;
    }

    private boolean isComplexConcept(String conceptUUID) {
        if (complexConcepts == null) {
            return false;
        }
        List<String> complexConceptUUIDs = Arrays.asList(complexConcepts.split(","));
        return complexConceptUUIDs.contains(conceptUUID);

    }

}
