package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Observation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.ehr.exception.EhrMapperException;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class NarrativeStatementMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/observation/";
    private static final String INPUT_JSON_WITH_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY + "example-observation-resource-1.json";
    private static final String INPUT_JSON_WITH_NULL_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY + "example-observation-resource-2.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD = TEST_FILE_DIRECTORY + "example-observation-resource-3.json";
    private static final String INPUT_JSON_WITH_ISSUED_ONLY = TEST_FILE_DIRECTORY + "example-observation-resource-4.json";
    private static final String INPUT_JSON_WITH_NO_DATES = TEST_FILE_DIRECTORY + "example-observation-resource-5.json";
    private static final String OUTPUT_XML_USES_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-1.xml";
    private static final String OUTPUT_XML_USES_ISSUED = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-2.xml";
    private static final String OUTPUT_XML_USES_EFFECTIVE_PERIOD_START = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-3.xml";
    private static final String OUTPUT_XML_USES_NESTED_COMPONENT = TEST_FILE_DIRECTORY + "expected-output-narrative-statement-4.xml";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private CharSequence expectedOutputMessage;
    private NarrativeStatementMapper narrativeStatementMapper;
    private MessageContext messageContext;

    @BeforeAll
    public static void initialize() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        narrativeStatementMapper = new NarrativeStatementMapper(messageContext);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @AfterAll
    public static void deinitialize() {
        TimeZone.setDefault(null);
    }

    @ParameterizedTest
    @MethodSource("resourceFileParams")
    public void When_MappingObservationJson_Expect_NarrativeStatementXmlOutput(String inputJson, String outputXml) throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(outputXml);
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME, OUTPUT_XML_USES_EFFECTIVE_DATE_TIME),
            Arguments.of(INPUT_JSON_WITH_NULL_EFFECTIVE_DATE_TIME, OUTPUT_XML_USES_ISSUED),
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_PERIOD, OUTPUT_XML_USES_EFFECTIVE_PERIOD_START),
            Arguments.of(INPUT_JSON_WITH_ISSUED_ONLY, OUTPUT_XML_USES_ISSUED)
        );
    }

    @Test
    public void When_MappingObservationJsonWithNestedTrue_Expect_NarrativeStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_NESTED_COMPONENT);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, true);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithNoDates_Expect_MapperException() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NO_DATES);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        assertThrows(EhrMapperException.class, ()
            -> narrativeStatementMapper.mapObservationToNarrativeStatement(parsedObservation, true));
    }
}
