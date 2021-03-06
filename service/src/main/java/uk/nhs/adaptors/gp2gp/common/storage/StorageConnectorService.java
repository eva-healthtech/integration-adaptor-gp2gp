package uk.nhs.adaptors.gp2gp.common.storage;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class StorageConnectorService {
    private final StorageConnector storageConnector;
    private final ObjectMapper objectMapper;

    @SneakyThrows(JsonProcessingException.class)
    public void uploadFile(StorageDataWrapper response, String fileName) {
        String jsonStringResponse = objectMapper.writeValueAsString(response);
        var responseBytes = jsonStringResponse.getBytes(UTF_8);
        var responseInputStream = new ByteArrayInputStream(responseBytes);

        storageConnector.uploadToStorage(responseInputStream, responseBytes.length, fileName);
    }

    @SneakyThrows
    public StorageDataWrapper downloadFile(String filename) {
        var inputStream = storageConnector.downloadFromStorage(filename);
        var stringDownload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        var storageDataWrapper = objectMapper.readValue(stringDownload, StorageDataWrapper.class);

        return storageDataWrapper;
    }
}
