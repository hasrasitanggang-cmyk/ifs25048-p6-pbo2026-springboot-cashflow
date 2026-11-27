package org.delcom.ifs25048springbootcashflow.adapter.ai;

import org.delcom.ifs25048springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25048springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25048springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25048springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25048springbootcashflow.domain.repository.IFinanceAiService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenAiFinanceService
        implements IFinanceAiService {

    private final String endpoint;
    private final String apiKey;
    private final String model;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiFinanceService(
            String endpoint,
            String apiKey,
            String model
    ) {
        this(
            endpoint,
            apiKey,
            model,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build(),
            new ObjectMapper()
        );
    }

    public OpenAiFinanceService(
            String endpoint,
            String apiKey,
            String model,
            HttpClient httpClient,
            ObjectMapper objectMapper
    ) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AiGeneratedTransaction> generateTransactions(
            String prompt
    ) throws DomainException {

        validateConfiguration(prompt);

        try {

            String requestBody = buildRequestBody(
                    prompt.trim()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                        normalizeEndpoint(endpoint)
                    ))
                    .header(
                        "Content-Type",
                        "application/json"
                    )
                    .header(
                        "Authorization",
                        "Bearer " + apiKey.trim()
                    )
                    .timeout(Duration.ofSeconds(60))
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            requestBody
                        )
                    )
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                    );

            int status = response.statusCode();

            if (status < 200 || status >= 300) {

                throw new DomainException(
                    "Layanan AI gagal. HTTP status: "
                        + status
                );
            }

            return parseResponse(response.body());

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new DomainException(
                "Proses Generate AI terinterupsi"
            );

        } catch (IOException ex) {

            throw new DomainException(
                "Tidak dapat terhubung ke server AI"
            );

        } catch (DomainException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new DomainException(
                "Gagal memproses permintaan AI"
            );
        }
    }

    private void validateConfiguration(
            String prompt
    ) throws ValidationException {

        if (prompt == null || prompt.isBlank()) {

            throw new ValidationException(
                "Prompt catatan keuangan tidak boleh kosong"
            );
        }

        if (endpoint == null || endpoint.isBlank()) {

            throw new ValidationException(
                "OPENAI_ENDPOINT belum dikonfigurasi"
            );
        }

        if (apiKey == null
                || apiKey.isBlank()
                || apiKey.trim().equalsIgnoreCase("key")) {

            throw new ValidationException(
                "OPENAI_KEY belum dikonfigurasi"
            );
        }

        if (model == null
                || model.isBlank()
                || model.trim().equalsIgnoreCase("nama-model")) {

            throw new ValidationException(
                "OPENAI_MODEL belum dikonfigurasi"
            );
        }
    }

    private String normalizeEndpoint(String value) {

        String url = value.trim();

        while (url.endsWith("/")) {

            url = url.substring(
                0,
                url.length() - 1
            );
        }

        if (url.endsWith("/chat/completions")) {
            return url;
        }

        return url + "/chat/completions";
    }

    private String buildRequestBody(
            String prompt
    ) throws DomainException {

        String instruction = """
            Anda adalah asisten pencatatan keuangan.

            Ekstrak transaksi keuangan yang disebutkan
            secara eksplisit oleh pengguna.

            Kembalikan hanya JSON array yang valid.

            Setiap objek wajib memiliki:
            - description: keterangan transaksi
            - amount: nominal rupiah berupa bilangan bulat positif
            - type: INCOME atau EXPENSE

            INCOME berarti pemasukan.
            EXPENSE berarti pengeluaran.

            Jangan menggunakan tanda Rp atau pemisah ribuan
            pada nilai amount.

            Jangan membuat transaksi atau nominal
            yang tidak disebutkan oleh pengguna.

            Jika tidak ada transaksi yang dapat diekstrak,
            kembalikan array kosong.

            Contoh:
            [
                {
                    "description": "Uang bulanan",
                    "amount": 1000000,
                    "type": "INCOME"
                },
                {
                    "description": "Makan siang",
                    "amount": 25000,
                    "type": "EXPENSE"
                }
            ]
            """;

        try {

            Map<String, Object> request = Map.of(
                "model", model.trim(),
                "messages", List.of(
                    Map.of(
                        "role", "system",
                        "content", instruction
                    ),
                    Map.of(
                        "role", "user",
                        "content", prompt
                    )
                )
            );

            return objectMapper.writeValueAsString(
                    request
            );

        } catch (Exception ex) {

            throw new DomainException(
                "Gagal menyusun JSON request AI"
            );
        }
    }

    private List<AiGeneratedTransaction> parseResponse(
            String responseBody
    ) throws DomainException {

        try {

            JsonNode root =
                    objectMapper.readTree(responseBody);

            JsonNode choices = root.get("choices");

            if (choices == null
                    || !choices.isArray()
                    || choices.isEmpty()) {

                throw new DomainException(
                    "Respons AI tidak memiliki choices"
                );
            }

            JsonNode contentNode = choices.get(0)
                    .path("message")
                    .path("content");

            if (!contentNode.isTextual()
                    || contentNode.asText().isBlank()) {

                throw new DomainException(
                    "Konten respons AI kosong"
                );
            }

            String content = removeMarkdownFence(
                contentNode.asText()
            );

            JsonNode transactions =
                    objectMapper.readTree(content);

            if (!transactions.isArray()) {

                throw new DomainException(
                    "Hasil AI harus berupa JSON array"
                );
            }

            List<AiGeneratedTransaction> result =
                    new ArrayList<>();

            for (JsonNode item : transactions) {

                JsonNode descriptionNode =
                        item.path("description");

                JsonNode amountNode =
                        item.path("amount");

                JsonNode typeNode =
                        item.path("type");

                if (!descriptionNode.isTextual()
                        || descriptionNode.asText().isBlank()) {

                    throw new DomainException(
                        "Keterangan transaksi AI tidak valid"
                    );
                }

                if (!amountNode.isIntegralNumber()
                        || !amountNode.canConvertToLong()) {

                    throw new DomainException(
                        "Nominal transaksi AI tidak valid"
                    );
                }

                long amount = amountNode.asLong();

                if (amount <= 0) {

                    throw new DomainException(
                        "Nominal transaksi AI harus positif"
                    );
                }

                if (!typeNode.isTextual()) {

                    throw new DomainException(
                        "Tipe transaksi AI tidak valid"
                    );
                }

                String description =
                        descriptionNode.asText().trim();

                String typeValue = typeNode.asText()
                        .trim()
                        .toUpperCase(Locale.ROOT);

                TransactionType type;

                try {

                    type = TransactionType.valueOf(
                            typeValue
                    );

                } catch (IllegalArgumentException ex) {

                    throw new DomainException(
                        "Tipe transaksi harus INCOME atau EXPENSE"
                    );
                }

                result.add(
                    new AiGeneratedTransaction(
                        description,
                        amount,
                        type
                    )
                );
            }

            if (result.isEmpty()) {

                throw new DomainException(
                    "AI tidak menghasilkan transaksi yang valid"
                );
            }

            return List.copyOf(result);

        } catch (DomainException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new DomainException(
                "Format respons JSON AI tidak dapat diproses"
            );
        }
    }

    private String removeMarkdownFence(String content) {

        String result = content.trim();

        if (result.startsWith("```")) {

            int firstNewLine = result.indexOf('\n');

            if (firstNewLine >= 0) {

                result = result.substring(
                    firstNewLine + 1
                );
            }

            result = result.trim();

            if (result.endsWith("```")) {

                result = result.substring(
                    0,
                    result.length() - 3
                );
            }
        }

        return result.trim();
    }
}