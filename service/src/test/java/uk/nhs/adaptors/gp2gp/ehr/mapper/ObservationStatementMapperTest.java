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
public class ObservationStatementMapperTest {
    private static final String TEST_ID = "394559384658936";
    private static final String TEST_FILE_DIRECTORY = "/ehr/mapper/observation/";
    private static final String INPUT_JSON_WITH_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY + "example-observation-resource-1.json";
    private static final String INPUT_JSON_WITH_NULL_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY + "example-observation-resource-2.json";
    private static final String INPUT_JSON_WITH_EFFECTIVE_PERIOD = TEST_FILE_DIRECTORY + "example-observation-resource-3.json";
    private static final String INPUT_JSON_WITH_ISSUED_ONLY = TEST_FILE_DIRECTORY + "example-observation-resource-4.json";
    private static final String INPUT_JSON_WITH_NO_DATES = TEST_FILE_DIRECTORY + "example-observation-resource-5.json";
    private static final String OUTPUT_XML_USES_EFFECTIVE_DATE_TIME = TEST_FILE_DIRECTORY + "expected-output-observation-statement-1.xml";
    private static final String OUTPUT_XML_USES_UNK_DATE_TIME = TEST_FILE_DIRECTORY + "expected-output-observation-statement-2.xml";
    private static final String OUTPUT_XML_USES_NESTED_COMPONENT = TEST_FILE_DIRECTORY + "expected-output-observation-statement-3.xml";
    private static final String OUTPUT_XML_USES_EFFECTIVE_PERIOD = TEST_FILE_DIRECTORY + "expected-output-observation-statement-4.xml";

    private CharSequence expectedOutputMessage;
    private ObservationStatementMapper observationStatementMapper;
    private MessageContext messageContext;

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    @BeforeAll
    public static void initialize() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        observationStatementMapper = new ObservationStatementMapper(messageContext);
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
    public void When_MappingObservationJson_Expect_ObservationStatementXmlOutput(String inputJson, String outputXml) throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(outputXml);
        var jsonInput = ResourceTestFileUtils.getFileContent(inputJson);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationStatementMapper.mapObservationToObservationStatement(parsedObservation, false);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    private static Stream<Arguments> resourceFileParams() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME, OUTPUT_XML_USES_EFFECTIVE_DATE_TIME),
            Arguments.of(INPUT_JSON_WITH_NULL_EFFECTIVE_DATE_TIME, OUTPUT_XML_USES_UNK_DATE_TIME),
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_PERIOD, OUTPUT_XML_USES_EFFECTIVE_PERIOD),
            Arguments.of(INPUT_JSON_WITH_ISSUED_ONLY, OUTPUT_XML_USES_UNK_DATE_TIME)
        );
    }

    @Test
    public void When_MappingParsedObservationJsonWithNestedTrue_Expect_ObservationStatementXmlOutput() throws IOException {
        expectedOutputMessage = ResourceTestFileUtils.getFileContent(OUTPUT_XML_USES_NESTED_COMPONENT);
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_EFFECTIVE_DATE_TIME);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        String outputMessage = observationStatementMapper.mapObservationToObservationStatement(parsedObservation, true);

        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    @Test
    public void When_MappingParsedObservationJsonWithNoDates_Expect_Exception() throws IOException {
        var jsonInput = ResourceTestFileUtils.getFileContent(INPUT_JSON_WITH_NO_DATES);
        Observation parsedObservation = new FhirParseService().parseResource(jsonInput, Observation.class);

        assertThrows(EhrMapperException.class, ()
            -> observationStatementMapper.mapObservationToObservationStatement(parsedObservation, true));
    }
}
