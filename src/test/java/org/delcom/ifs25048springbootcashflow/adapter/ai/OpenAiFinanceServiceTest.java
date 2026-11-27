
package org.delcom.ifs25048springbootcashflow.adapter.ai;

import org.delcom.ifs25048springbootcashflow.domain.entity.*;
import org.delcom.ifs25048springbootcashflow.domain.exception.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;

import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class OpenAiFinanceServiceTest {

    private HttpServer server;
    private String endpoint;

    private final ObjectMapper mapper = new ObjectMapper();

    private volatile int statusCode = 200;
    private volatile String responseBody = "{}";

    private static final String VALID_JSON = """
        [
            {
                "description": "Gaji",
                "amount": 1000000,
                "type": "INCOME"
            },
            {
                "description": "Makan",
                "amount": 25000,
                "type": "EXPENSE"
            }
        ]
        """;

    @BeforeEach
    void setUp() throws Exception {

        server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0), 0
        );

        server.createContext(
            "/v1/chat/completions",
            exchange -> {

                try {

                    exchange.getRequestBody().readAllBytes();

                    byte[] bytes = responseBody.getBytes(
                        StandardCharsets.UTF_8
                    );

                    exchange.getResponseHeaders().set(
                        "Content-Type",
                        "application/json"
                    );

                    exchange.sendResponseHeaders(
                        statusCode,
                        bytes.length
                    );

                    exchange.getResponseBody().write(bytes);

                } finally {
                    exchange.close();
                }
            }
        );

        server.start();

        endpoint = "http://127.0.0.1:"
            + server.getAddress().getPort()
            + "/v1";
    }

    @AfterEach
    void tearDown() {

        Thread.interrupted();

        if (server != null) {
            server.stop(0);
        }
    }

    private OpenAiFinanceService service() {

        return new OpenAiFinanceService(
            endpoint,
            "dummy-test-key",
            "dummy-test-model"
        );
    }

    private void setContent(String content) throws Exception {

        responseBody = mapper.writeValueAsString(
            Map.of(
                "choices",
                List.of(
                    Map.of(
                        "message",
                        Map.of("content", content)
                    )
                )
            )
        );
    }

    private void assertDomainError(String expectedMessage) {

        DomainException exception = assertThrows(
            DomainException.class,
            () -> service().generateTransactions("Test")
        );

        assertEquals(
            expectedMessage,
            exception.getMessage()
        );
    }

    // ==========================================
    // SUCCESSFUL RESPONSE
    // ==========================================

    @Test
    void testSuccessfulResponse() throws Exception {

        setContent(VALID_JSON);

        var result = service().generateTransactions(
            "Gaji dan makan"
        );

        assertEquals(2, result.size());

        assertEquals("Gaji", result.get(0).description());
        assertEquals(1000000L, result.get(0).amount());
        assertEquals(TransactionType.INCOME, result.get(0).type());

        assertEquals("Makan", result.get(1).description());
        assertEquals(25000L, result.get(1).amount());
        assertEquals(TransactionType.EXPENSE, result.get(1).type());
    }

    @Test
    void testTrimmedDescriptionAndLowercaseType()
        throws Exception {

        setContent("""
            [
                {
                    "description": "  Uang saku  ",
                    "amount": 500000,
                    "type": " income "
                }
            ]
            """);

        var result = service().generateTransactions(
            "Uang saku"
        );

        assertEquals(1, result.size());
        assertEquals("Uang saku", result.get(0).description());
        assertEquals(500000L, result.get(0).amount());
        assertEquals(TransactionType.INCOME, result.get(0).type());
    }

    // ==========================================
    // ENDPOINT NORMALIZATION
    // ==========================================

    @Test
    void testEndpointWithTrailingSlash()
        throws Exception {

        setContent(VALID_JSON);

        var ai = new OpenAiFinanceService(
            endpoint + "/",
            "dummy-test-key",
            "dummy-test-model"
        );

        assertEquals(
            2,
            ai.generateTransactions("Test").size()
        );
    }

    @Test
    void testEndpointWithMultipleTrailingSlashes()
        throws Exception {

        setContent(VALID_JSON);

        var ai = new OpenAiFinanceService(
            endpoint + "///",
            "dummy-test-key",
            "dummy-test-model"
        );

        assertEquals(
            2,
            ai.generateTransactions("Test").size()
        );
    }

    @Test
    void testCompleteEndpoint()
        throws Exception {

        setContent(VALID_JSON);

        var ai = new OpenAiFinanceService(
            endpoint + "/chat/completions",
            "dummy-test-key",
            "dummy-test-model"
        );

        assertEquals(
            2,
            ai.generateTransactions("Test").size()
        );
    }

    @Test
    void testCompleteEndpointWithWhitespace()
        throws Exception {

        setContent(VALID_JSON);

        var ai = new OpenAiFinanceService(
            "  " + endpoint + "/chat/completions/  ",
            " dummy-test-key ",
            " dummy-test-model "
        );

        assertEquals(
            2,
            ai.generateTransactions("  Test  ").size()
        );
    }

    // ==========================================
    // MARKDOWN RESPONSES
    // ==========================================

    @Test
    void testMarkdownResponse() throws Exception {

        setContent(
            "```json\n"
            + VALID_JSON
            + "\n```"
        );

        assertEquals(
            2,
            service().generateTransactions("Test").size()
        );
    }

    @Test
    void testMarkdownWithoutLanguage()
        throws Exception {

        setContent(
            "```\n"
            + VALID_JSON
            + "\n```"
        );

        assertEquals(
            2,
            service().generateTransactions("Test").size()
        );
    }

    @Test
    void testMarkdownWithoutClosingFence()
        throws Exception {

        setContent(
            "```json\n" + VALID_JSON
        );

        assertEquals(
            2,
            service().generateTransactions("Test").size()
        );
    }

    @Test
    void testMarkdownWithoutNewLine()
        throws Exception {

        setContent("```json");

        assertDomainError(
            "Format respons JSON AI tidak dapat diproses"
        );
    }

    // ==========================================
    // HTTP STATUS ERRORS
    // ==========================================

    @Test
    void testHttp400() {

        statusCode = 400;

        assertDomainError(
            "Layanan AI gagal. HTTP status: 400"
        );
    }

    @Test
    void testHttp500() {

        statusCode = 500;

        assertDomainError(
            "Layanan AI gagal. HTTP status: 500"
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHttpStatusBelow200() throws Exception {

        HttpClient client = mock(HttpClient.class);

        HttpResponse<String> response =
            mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(199);

        when(client.send(
            any(HttpRequest.class),
            ArgumentMatchers
                .<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(response);

        var ai = new OpenAiFinanceService(
            endpoint,
            "dummy-test-key",
            "dummy-test-model",
            client,
            mapper
        );

        DomainException exception = assertThrows(
            DomainException.class,
            () -> ai.generateTransactions("Test")
        );

        assertEquals(
            "Layanan AI gagal. HTTP status: 199",
            exception.getMessage()
        );
    }

    // ==========================================
    // INVALID CHOICES
    // ==========================================

    @Test
    void testMissingChoices() {

        responseBody = "{}";

        assertDomainError(
            "Respons AI tidak memiliki choices"
        );
    }

    @Test
    void testEmptyChoices() {

        responseBody = "{\"choices\":[]}";

        assertDomainError(
            "Respons AI tidak memiliki choices"
        );
    }

    @Test
    void testChoicesIsNotArray() {

        responseBody = "{\"choices\":{}}";

        assertDomainError(
            "Respons AI tidak memiliki choices"
        );
    }

    @Test
    void testChoicesIsNull() {

        responseBody = "{\"choices\":null}";

        assertDomainError(
            "Respons AI tidak memiliki choices"
        );
    }

    // ==========================================
    // INVALID CONTENT
    // ==========================================

    @Test
    void testEmptyContent() throws Exception {

        setContent("");

        assertDomainError(
            "Konten respons AI kosong"
        );
    }

    @Test
    void testBlankContent() throws Exception {

        setContent("   ");

        assertDomainError(
            "Konten respons AI kosong"
        );
    }

    @Test
    void testMissingContent() {

        responseBody = """
            {
                "choices": [
                    {
                        "message": {}
                    }
                ]
            }
            """;

        assertDomainError(
            "Konten respons AI kosong"
        );
    }

    @Test
    void testContentIsNotText() {

        responseBody = """
            {
                "choices": [
                    {
                        "message": {
                            "content": 123
                        }
                    }
                ]
            }
            """;

        assertDomainError(
            "Konten respons AI kosong"
        );
    }

    // ==========================================
    // INVALID JSON
    // ==========================================

    @Test
    void testInvalidJson() throws Exception {

        setContent("Bukan JSON");

        assertDomainError(
            "Format respons JSON AI tidak dapat diproses"
        );
    }

    @Test
    void testInvalidOuterJson() {

        responseBody = "invalid JSON";

        assertDomainError(
            "Format respons JSON AI tidak dapat diproses"
        );
    }

    @Test
    void testNotArray() throws Exception {

        setContent("{}");

        assertDomainError(
            "Hasil AI harus berupa JSON array"
        );
    }

    @Test
    void testEmptyArray() throws Exception {

        setContent("[]");

        assertDomainError(
            "AI tidak menghasilkan transaksi yang valid"
        );
    }

    // ==========================================
    // INVALID TRANSACTION DESCRIPTION
    // ==========================================

    @Test
    void testInvalidDescription()
        throws Exception {

        String[] invalid = {

            """
            [{"description":"","amount":1000,"type":"INCOME"}]
            """,

            """
            [{"description":"   ","amount":1000,"type":"INCOME"}]
            """,

            """
            [{"description":123,"amount":1000,"type":"INCOME"}]
            """,

            """
            [{"description":null,"amount":1000,"type":"INCOME"}]
            """,

            """
            [{"amount":1000,"type":"INCOME"}]
            """
        };

        for (String content : invalid) {

            setContent(content);

            assertDomainError(
                "Keterangan transaksi AI tidak valid"
            );
        }
    }

    // ==========================================
    // INVALID TRANSACTION AMOUNT
    // ==========================================

    @Test
    void testInvalidAmount()
        throws Exception {

        String[] invalid = {

            """
            [{"description":"Test","amount":"abc","type":"INCOME"}]
            """,

            """
            [{"description":"Test","amount":12.5,"type":"INCOME"}]
            """,

            """
            [{"description":"Test","amount":null,"type":"INCOME"}]
            """,

            """
            [{"description":"Test","type":"INCOME"}]
            """,

            """
            [{"description":"Test","amount":9223372036854775808,"type":"INCOME"}]
            """
        };

        for (String content : invalid) {

            setContent(content);

            assertDomainError(
                "Nominal transaksi AI tidak valid"
            );
        }
    }

    @Test
    void testNegativeAndZeroAmount()
        throws Exception {

        setContent("""
            [
                {
                    "description": "Test",
                    "amount": -1000,
                    "type": "EXPENSE"
                }
            ]
            """);

        assertDomainError(
            "Nominal transaksi AI harus positif"
        );

        setContent("""
            [
                {
                    "description": "Test",
                    "amount": 0,
                    "type": "INCOME"
                }
            ]
            """);

        assertDomainError(
            "Nominal transaksi AI harus positif"
        );
    }

    // ==========================================
    // INVALID TRANSACTION TYPE
    // ==========================================

    @Test
    void testInvalidTransactionType()
        throws Exception {

        setContent("""
            [
                {
                    "description": "Test",
                    "amount": 1000,
                    "type": "UNKNOWN"
                }
            ]
            """);

        assertDomainError(
            "Tipe transaksi harus INCOME atau EXPENSE"
        );

        setContent("""
            [
                {
                    "description": "Test",
                    "amount": 1000
                }
            ]
            """);

        assertDomainError(
            "Tipe transaksi AI tidak valid"
        );

        setContent("""
            [
                {
                    "description": "Test",
                    "amount": 1000,
                    "type": 123
                }
            ]
            """);

        assertDomainError(
            "Tipe transaksi AI tidak valid"
        );
    }

    // ==========================================
    // VALIDATION: PROMPT
    // ==========================================

    @Test
    void testInvalidPrompt() {

        String[] invalid = {
            null, "", "   "
        };

        for (String prompt : invalid) {

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service().generateTransactions(prompt)
            );

            assertEquals(
                "Prompt catatan keuangan tidak boleh kosong",
                exception.getMessage()
            );
        }
    }

    // ==========================================
    // VALIDATION: ENDPOINT
    // ==========================================

    @Test
    void testInvalidEndpoint() {

        String[] invalid = {
            null, "", "   "
        };

        for (String value : invalid) {

            var ai = new OpenAiFinanceService(
                value,
                "dummy-test-key",
                "dummy-test-model"
            );

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> ai.generateTransactions("Test")
            );

            assertEquals(
                "OPENAI_ENDPOINT belum dikonfigurasi",
                exception.getMessage()
            );
        }
    }

    // ==========================================
    // VALIDATION: API KEY
    // ==========================================

    @Test
    void testInvalidApiKey() {

        String[] invalid = {
            null,
            "",
            "   ",
            "key",
            " KEY "
        };

        for (String key : invalid) {

            var ai = new OpenAiFinanceService(
                endpoint,
                key,
                "dummy-test-model"
            );

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> ai.generateTransactions("Test")
            );

            assertEquals(
                "OPENAI_KEY belum dikonfigurasi",
                exception.getMessage()
            );
        }
    }

    // ==========================================
    // VALIDATION: MODEL
    // ==========================================

    @Test
    void testInvalidModel() {

        String[] invalid = {
            null,
            "",
            "   ",
            "nama-model",
            " NAMA-MODEL "
        };

        for (String model : invalid) {

            var ai = new OpenAiFinanceService(
                endpoint,
                "dummy-test-key",
                model
            );

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> ai.generateTransactions("Test")
            );

            assertEquals(
                "OPENAI_MODEL belum dikonfigurasi",
                exception.getMessage()
            );
        }
    }

    // ==========================================
    // IO EXCEPTION
    // ==========================================

    @Test
    void testIOException()
        throws Exception {

        HttpClient client = mock(HttpClient.class);

        when(client.send(
            any(HttpRequest.class),
            ArgumentMatchers
                .<HttpResponse.BodyHandler<String>>any()
        )).thenThrow(
            new IOException("Simulated connection error")
        );

        var ai = new OpenAiFinanceService(
            endpoint,
            "dummy-test-key",
            "dummy-test-model",
            client,
            mapper
        );

        DomainException exception = assertThrows(
            DomainException.class,
            () -> ai.generateTransactions("Test")
        );

        assertEquals(
            "Tidak dapat terhubung ke server AI",
            exception.getMessage()
        );
    }

    // ==========================================
    // INTERRUPTED EXCEPTION
    // ==========================================

    @Test
    void testInterruptedException()
        throws Exception {

        HttpClient client = mock(HttpClient.class);

        when(client.send(
            any(HttpRequest.class),
            ArgumentMatchers
                .<HttpResponse.BodyHandler<String>>any()
        )).thenThrow(
            new InterruptedException("Simulated interruption")
        );

        var ai = new OpenAiFinanceService(
            endpoint,
            "dummy-test-key",
            "dummy-test-model",
            client,
            mapper
        );

        try {

            DomainException exception = assertThrows(
                DomainException.class,
                () -> ai.generateTransactions("Test")
            );

            assertEquals(
                "Proses Generate AI terinterupsi",
                exception.getMessage()
            );

            assertTrue(
                Thread.currentThread().isInterrupted()
            );

        } finally {

            Thread.interrupted();
        }
    }

    // ==========================================
    // UNEXPECTED EXCEPTION
    // ==========================================

    @Test
    void testUnexpectedException()
        throws Exception {

        HttpClient client = mock(HttpClient.class);

        when(client.send(
            any(HttpRequest.class),
            ArgumentMatchers
                .<HttpResponse.BodyHandler<String>>any()
        )).thenThrow(
            new IllegalStateException("Unexpected error")
        );

        var ai = new OpenAiFinanceService(
            endpoint,
            "dummy-test-key",
            "dummy-test-model",
            client,
            mapper
        );

        DomainException exception = assertThrows(
            DomainException.class,
            () -> ai.generateTransactions("Test")
        );

        assertEquals(
            "Gagal memproses permintaan AI",
            exception.getMessage()
        );
    }

    // ==========================================
    // JSON REQUEST SERIALIZATION ERROR
    // ==========================================

    @Test
    void testBuildRequestBodyFailure()
        throws Exception {

        HttpClient client = mock(HttpClient.class);

        ObjectMapper mockedMapper =
            mock(ObjectMapper.class);

        when(mockedMapper.writeValueAsString(any()))
            .thenThrow(
                new IllegalStateException(
                    "Simulated JSON serialization error"
                )
            );

        var ai = new OpenAiFinanceService(
            endpoint,
            "dummy-test-key",
            "dummy-test-model",
            client,
            mockedMapper
        );

        DomainException exception = assertThrows(
            DomainException.class,
            () -> ai.generateTransactions("Test")
        );

        assertEquals(
            "Gagal menyusun JSON request AI",
            exception.getMessage()
        );

        verifyNoInteractions(client);
    }
}
