package uk.nhs.adaptors.gp2gp.ehr.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Encounter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.nhs.adaptors.gp2gp.common.service.FhirParseService;
import uk.nhs.adaptors.gp2gp.common.service.RandomIdGeneratorService;
import uk.nhs.adaptors.gp2gp.utils.ResourceTestFileUtils;

@ExtendWith(MockitoExtension.class)
public class EncounterStatementMapperTest {
    private static final String TEST_FILES_DIRECTORY = "/ehr/mapper/encounter/";
    private static final String INPUT_JSON_WITH_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "example-encounter-resource-1.json";
    private static final String OUTPUT_XML_WITH_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "expected-output-encounter-narrative-statement-1.xml";
    private static final String INPUT_JSON_WITH_START_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "example-encounter-resource-2.json";
    private static final String OUTPUT_XML_WITH_START_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "expected-output-encounter-narrative-statement-2.xml";
    private static final String INPUT_JSON_WITH_NO_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "example-encounter-resource-3.json";
    private static final String OUTPUT_XML_WITH_NO_EFFECTIVE_TIME = TEST_FILES_DIRECTORY
        + "expected-output-encounter-narrative-statement-3.xml";
    private static final String INPUT_JSON_WITH_NO_PERIOD_FIELD = TEST_FILES_DIRECTORY
        + "example-encounter-resource-4.json";
    private static final String OUTPUT_XML_WITH_NO_PERIOD_FIELD = TEST_FILES_DIRECTORY
        + "expected-output-encounter-narrative-statement-4.xml";
    private static final String TEST_ID = "test-id";

    @Mock
    private RandomIdGeneratorService randomIdGeneratorService;

    private EncounterStatementMapper encounterStatementMapper;
    private MessageContext messageContext;

    @BeforeAll
    public static void initialize() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }

    @AfterAll
    public static void deinitialize() {
        TimeZone.setDefault(null);
    }

    @BeforeEach
    public void setUp() {
        when(randomIdGeneratorService.createNewId()).thenReturn(TEST_ID);
        messageContext = new MessageContext(randomIdGeneratorService);
        encounterStatementMapper = new EncounterStatementMapper(messageContext);
    }

    @AfterEach
    public void tearDown() {
        messageContext.resetMessageContext();
    }

    @ParameterizedTest
    @MethodSource("testFilePaths")
    public void When_MappingParsedEncounterJson_Expect_EncounterStatementXmlOutput(String input, String output) throws IOException {
        String expectedOutputMessage = ResourceTestFileUtils.getFileContent(output);

        var jsonInput = ResourceTestFileUtils.getFileContent(input);
        Encounter parsedEncounter = new FhirParseService().parseResource(jsonInput, Encounter.class);

        String outputMessage = encounterStatementMapper.mapEncounterToEncounterStatement(parsedEncounter);
        assertThat(outputMessage).isEqualToIgnoringWhitespace(expectedOutputMessage);
    }

    private static Stream<Arguments> testFilePaths() {
        return Stream.of(
            Arguments.of(INPUT_JSON_WITH_EFFECTIVE_TIME, OUTPUT_XML_WITH_EFFECTIVE_TIME),
            Arguments.of(INPUT_JSON_WITH_START_EFFECTIVE_TIME, OUTPUT_XML_WITH_START_EFFECTIVE_TIME),
            Arguments.of(INPUT_JSON_WITH_NO_EFFECTIVE_TIME, OUTPUT_XML_WITH_NO_EFFECTIVE_TIME),
            Arguments.of(INPUT_JSON_WITH_NO_PERIOD_FIELD, OUTPUT_XML_WITH_NO_PERIOD_FIELD)
        );
    }
}
