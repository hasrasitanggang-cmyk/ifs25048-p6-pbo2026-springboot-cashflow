package org.delcom.ifs25048springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;

import org.delcom.ifs25048springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25048springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25048springbootcashflow.domain.entity.*;
import org.delcom.ifs25048springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25048springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25048springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25048springbootcashflow.usecase.FinanceAiUseCase;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Cashflow")
public class FinanceView extends VerticalLayout
        implements BeforeEnterObserver {

    private final FinanceUseCase financeUseCase;
    private final FinanceAiUseCase financeAiUseCase;
    private final FinancePresenter presenter;
    private final FileStorage fileStorage;
    private final IUserRepository userRepository;

    private final Grid<Transaction> grid =
            new Grid<>(Transaction.class, false);

    private final TextField search = new TextField();

    private final Select<TransactionTypeFilter> filter =
            new Select<>();

    private final Select<SortOption> sort =
            new Select<>();

    private final Span totalCount = new Span("0");
    private final Span totalIncome = new Span("Rp 0.00");
    private final Span totalExpense = new Span("Rp 0.00");
    private final Span balance = new Span("Rp 0.00");

    private final Span status = new Span();

    private record PendingImage(
        String name,
        byte[] data
    ) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PendingImage other)) {
                return false;
            }
            return java.util.Objects.equals(name, other.name)
                && java.util.Arrays.equals(data, other.data);
        }

        @Override
        public int hashCode() {
            return 31 * java.util.Objects.hashCode(name)
                + java.util.Arrays.hashCode(data);
        }

        @Override
        public String toString() {
            return "PendingImage[name=" + name
                + ", data=" + java.util.Arrays.toString(data) + "]";
        }
    }

    public FinanceView(
            FinanceUseCase financeUseCase,
            FinanceAiUseCase financeAiUseCase,
            FinancePresenter presenter,
            FileStorage fileStorage,
            IUserRepository userRepository
    ) {
        this.financeUseCase = financeUseCase;
        this.financeAiUseCase = financeAiUseCase;
        this.presenter = presenter;
        this.fileStorage = fileStorage;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        getStyle().set("background", "#F3F4F6");

        configureGrid();

        H1 title = new H1("Catatan Keuangan Saya");

        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.5rem");

        Div card = PageCard.wrap(
                title,
                createSummary(),
                createToolbar(),
                status,
                grid
        );

        add(card);
        expand(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        if (AuthSession.currentUser(userRepository)
                .isEmpty()) {

            event.forwardTo(LoginView.class);
            return;
        }

        refreshAll();
    }

    private int currentUserId() {

        return AuthSession.currentUser()
                .orElseThrow()
                .getId();
    }

    private HorizontalLayout createSummary() {

        HorizontalLayout layout = new HorizontalLayout(
                summaryCard(
                    "Total Transaksi",
                    totalCount,
                    "#DBEAFE",
                    "#2563EB"
                ),
                summaryCard(
                    "Total Pemasukan",
                    totalIncome,
                    "#DCFCE7",
                    "#16A34A"
                ),
                summaryCard(
                    "Total Pengeluaran",
                    totalExpense,
                    "#FEE2E2",
                    "#DC2626"
                ),
                summaryCard(
                    "Saldo",
                    balance,
                    "#FEF3C7",
                    "#D97706"
                )
        );

        layout.setWidthFull();

        layout.getStyle()
                .set("flex-wrap", "wrap");

        return layout;
    }

    private Div summaryCard(
            String label,
            Span value,
            String background,
            String color
    ) {

        Span caption = new Span(label);

        caption.getStyle()
                .set("display", "block")
                .set("font-size", "0.85rem")
                .set("color", color);

        value.getStyle()
                .set("display", "block")
                .set("font-size", "1.3rem")
                .set("font-weight", "700")
                .set("color", color);

        Div card = new Div(caption, value);

        card.getStyle()
                .set("flex", "1")
                .set("min-width", "160px")
                .set("background", background)
                .set("border", "1px solid " + color)
                .set("border-left", "4px solid " + color)
                .set("border-radius", "10px")
                .set("padding", "12px");

        return card;
    }

    private HorizontalLayout createToolbar() {

        search.setPlaceholder("Cari keterangan...");
        search.setClearButtonVisible(true);

        filter.setItems(TransactionTypeFilter.values());
        filter.setItemLabelGenerator(
                TransactionTypeFilter::label
        );
        filter.setValue(TransactionTypeFilter.ALL);

        sort.setItems(SortOption.values());
        sort.setItemLabelGenerator(SortOption::label);
        sort.setValue(SortOption.NEWEST_FIRST);

        Button searchButton = new Button(
                "Cari",
                event -> refreshAll()
        );

        Button reload = new Button(
                "Muat Ulang",
                event -> {
                    search.clear();
                    filter.setValue(TransactionTypeFilter.ALL);
                    sort.setValue(SortOption.NEWEST_FIRST);
                    refreshAll();
                }
        );

        Button generate = new Button(
                "Generate AI",
                event -> openAiDialog()
        );

        generate.addThemeVariants(
                ButtonVariant.LUMO_SUCCESS
        );

        Button income = new Button(
                "+ Pemasukan",
                event -> openAddDialog(TransactionType.INCOME)
        );

        income.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_SUCCESS
        );

        Button expense = new Button(
                "+ Pengeluaran",
                event -> openAddDialog(TransactionType.EXPENSE)
        );

        expense.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_ERROR
        );

        filter.addValueChangeListener(event -> refreshAll());
        sort.addValueChangeListener(event -> refreshAll());

        for (Button button : List.of(
                searchButton, reload, generate, income, expense
        )) {
            button.getStyle().set("cursor", "pointer");
        }

        HorizontalLayout toolbar = new HorizontalLayout(
                search,
                filter,
                sort,
                searchButton,
                reload,
                generate,
                income,
                expense
        );

        toolbar.setWidthFull();

        toolbar.getStyle()
                .set("flex-wrap", "wrap")
                .set("align-items", "center");

        return toolbar;
    }

    private void configureGrid() {

        grid.addComponentColumn(transaction -> {

            String path = transaction.getProofPath();

            if (path == null || path.isBlank()) {
                return new Span("-");
            }

            return ImagePreviewDialog.clickableThumbnail(
                    "/uploads/" + path,
                    "Bukti transaksi",
                    "48px"
            );

        }).setHeader("Bukti").setAutoWidth(true);

        grid.addColumn(Transaction::getId)
                .setHeader("ID")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(Transaction::getDescription)
                .setHeader("Keterangan")
                .setSortable(true)
                .setFlexGrow(2);

        grid.addColumn(transaction ->
                presenter.formatAmount(transaction.getAmount())
        )
                .setHeader("Jumlah")
                .setComparator(Transaction::getAmount)
                .setSortable(true)
                .setAutoWidth(true);

        grid.addComponentColumn(transaction -> {

            Span badge = new Span(
                    transaction.getType().label()
            );

            badge.getStyle()
                    .set("color",
                            transaction.getType()
                                    == TransactionType.INCOME
                                    ? "#15803D"
                                    : "#DC2626")
                    .set("font-weight", "600");

            return badge;

        }).setHeader("Tipe").setAutoWidth(true);

        grid.addColumn(transaction ->
                presenter.formatDateTime(
                        transaction.getCreatedAt()
                )
        )
                .setHeader("Waktu")
                .setComparator(Transaction::getCreatedAt)
                .setSortable(true)
                .setAutoWidth(true);

        grid.addComponentColumn(transaction -> {

            Button delete = new Button(
                    "Hapus",
                    event -> openDeleteDialog(transaction)
            );

            delete.addThemeVariants(
                    ButtonVariant.LUMO_ERROR,
                    ButtonVariant.LUMO_TERTIARY
            );

            delete.getStyle().set("cursor", "pointer");

            return delete;

        }).setHeader("Tindakan").setAutoWidth(true);

        grid.setSizeFull();

        GridStyles.apply(grid);
    }

    private void refreshAll() {

        if (AuthSession.currentUser().isEmpty()) {
            return;
        }

        int userId = currentUserId();

        List<Transaction> transactions =
                financeUseCase.listTransactions(
                        userId,
                        search.getValue(),
                        filter.getValue(),
                        sort.getValue()
                );

        grid.setItems(transactions);

        totalCount.setText(
                String.valueOf(
                        financeUseCase.getTransactionCount(userId)
                )
        );

        totalIncome.setText(
                presenter.formatAmount(
                        financeUseCase.getTotalIncome(userId)
                )
        );

        totalExpense.setText(
                presenter.formatAmount(
                        financeUseCase.getTotalExpense(userId)
                )
        );

        long currentBalance =
                financeUseCase.getBalance(userId);

        balance.setText(
                presenter.formatAmount(currentBalance)
        );

        balance.getStyle().set(
                "color",
                currentBalance >= 0
                    ? "#15803D"
                    : "#DC2626"
        );

        status.setText(
                transactions.isEmpty()
                    ? "Data transaksi belum tersedia."
                    : "Menampilkan " + transactions.size()
                        + " transaksi"
        );
    }

    private void openAddDialog(TransactionType type) {

        Dialog dialog = new Dialog();

        TextArea description =
                new TextArea("Keterangan");

        TextField amount =
                new TextField("Jumlah (Rp)");

        description.setWidthFull();
        amount.setWidthFull();

        description.setRequiredIndicatorVisible(true);
        amount.setRequiredIndicatorVisible(true);

        amount.setPlaceholder("Contoh: 25000");

        AtomicReference<PendingImage> pending =
                new AtomicReference<>();

        Upload upload = new Upload(
                UploadHandler.inMemory((metadata, data) -> {

                    pending.set(new PendingImage(
                            metadata.fileName(),
                            data
                    ));
                })
        );

        upload.setAcceptedMimeTypes(
                "image/png",
                "image/jpeg"
        );

        upload.setAcceptedFileExtensions(
                ".png", ".jpg", ".jpeg"
        );

        upload.setMaxFiles(1);
        upload.setMaxFileSize(5 * 1024 * 1024);

        upload.addFileRemovedListener(event ->
                pending.set(null)
        );

        ProgressBar loading = new ProgressBar();
        loading.setIndeterminate(true);
        loading.setVisible(false);

        Button cancel = new Button(
                "Batal",
                event -> dialog.close()
        );

        Button save = new Button("Simpan");

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        save.addClickListener(event -> {

            loading.setVisible(true);
            save.setEnabled(false);

            try {

                long value;

                try {

                    value = Long.parseLong(
                            amount.getValue().trim()
                    );

                } catch (NumberFormatException ex) {

                    throw new IllegalArgumentException(
                            "Jumlah harus berupa angka bulat"
                    );
                }

                if (value <= 0) {
                    throw new IllegalArgumentException(
                            "Jumlah harus lebih besar dari nol"
                    );
                }

                if (description.getValue().isBlank()) {
                    throw new IllegalArgumentException(
                            "Keterangan tidak boleh kosong"
                    );
                }

                String proofPath = null;

                PendingImage image = pending.get();

                if (image != null) {

                    String name = image.name()
                            .toLowerCase(Locale.ROOT);

                    if (!(name.endsWith(".png")
                            || name.endsWith(".jpg")
                            || name.endsWith(".jpeg"))) {

                        throw new IllegalArgumentException(
                                "Format gambar tidak didukung"
                        );
                    }

                    if (image.data().length > 5 * 1024 * 1024) {

                        throw new IllegalArgumentException(
                                "Ukuran gambar maksimal 5 MB"
                        );
                    }

                    if (javax.imageio.ImageIO.read(
                            new ByteArrayInputStream(
                                    image.data()
                            )
                    ) == null) {

                        throw new IllegalArgumentException(
                                "File bukan gambar yang valid"
                        );
                    }

                    proofPath = fileStorage.store(
                            "proofs",
                            image.name(),
                            new ByteArrayInputStream(
                                    image.data()
                            )
                    );
                }

                Transaction saved =
                        financeUseCase.addTransaction(
                                currentUserId(),
                                description.getValue(),
                                value,
                                type,
                                proofPath
                        );

                success(presenter.successAdd(saved));

                dialog.close();

                refreshAll();

            } catch (Exception ex) {

                error(ex.getMessage());

            } finally {

                loading.setVisible(false);
                save.setEnabled(true);
            }
        });

        VerticalLayout body = new VerticalLayout(
                description,
                amount,
                new Span("Bukti transaksi (opsional)"),
                upload,
                loading
        );

        setupDialog(
                dialog,
                "Tambah " + type.label(),
                body,
                cancel,
                save
        );

        dialog.open();
    }

    private void openDeleteDialog(Transaction transaction) {

        Dialog dialog = new Dialog();

        Paragraph message = new Paragraph(
                "Yakin ingin menghapus transaksi "
                        + presenter.formatTransaction(transaction)
                        + "?"
        );

        Button cancel = new Button(
                "Batal",
                event -> dialog.close()
        );

        Button delete = new Button("Hapus");

        delete.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_ERROR
        );

        delete.addClickListener(event -> {

            delete.setEnabled(false);

            try {

                financeUseCase.removeTransactionForUser(
                        currentUserId(),
                        transaction.getId()
                );

                success(
                        presenter.successDelete(
                                transaction.getId()
                        )
                );

                dialog.close();

                refreshAll();

            } catch (Exception ex) {

                error(ex.getMessage());

                delete.setEnabled(true);
            }
        });

        setupDialog(
                dialog,
                "Konfirmasi Penghapusan",
                message,
                cancel,
                delete
        );

        dialog.open();
    }

    private void openAiDialog() {

        Dialog dialog = new Dialog();

        TextArea prompt =
                new TextArea("Catatan Keuangan");

        prompt.setWidthFull();

        prompt.setPlaceholder(
                "Contoh: menerima uang bulanan Rp1000000, "
                + "membeli makan siang Rp25000"
        );

        ProgressBar loading = new ProgressBar();

        loading.setIndeterminate(true);
        loading.setVisible(false);
        loading.setWidthFull();

        Grid<AiGeneratedTransaction> preview =
                new Grid<>(AiGeneratedTransaction.class, false);

        preview.addColumn(
                AiGeneratedTransaction::description
        ).setHeader("Keterangan");

        preview.addColumn(item ->
                presenter.formatAmount(item.amount())
        ).setHeader("Jumlah");

        preview.addColumn(item ->
                item.type().label()
        ).setHeader("Tipe");

        preview.setWidthFull();
        preview.setHeight("260px");
        preview.setVisible(false);

        GridStyles.apply(preview);

        AtomicReference<List<AiGeneratedTransaction>> generated =
                new AtomicReference<>(List.of());

        Button generate = new Button("Proses AI");

        Button cancel = new Button(
                "Batal",
                event -> dialog.close()
        );

        Button apply = new Button("Tambahkan ke Catatan");

        apply.setEnabled(false);

        apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        generate.addClickListener(event -> {

            String text = prompt.getValue().trim();

            if (text.isBlank()) {

                error("Prompt tidak boleh kosong");
                return;
            }

            generated.set(List.of());

            preview.setItems(List.of());
            preview.setVisible(false);

            loading.setVisible(true);

            generate.setEnabled(false);
            apply.setEnabled(false);
            cancel.setEnabled(false);
            prompt.setReadOnly(true);

            UI ui = UI.getCurrent();

            int previousPollInterval = ui.getPollInterval();

            ui.setPollInterval(500);

            CompletableFuture.supplyAsync(() -> {

                try {

                    return financeAiUseCase
                            .generateTransactions(text);

                } catch (Exception ex) {

                    throw new CompletionException(ex);
                }

            }).whenComplete((items, failure) -> {

                if (!ui.isAttached()) {
                    return;
                }

                ui.access(() -> {

                    try {

                        if (!dialog.isOpened()) {
                            return;
                        }

                        if (failure != null) {

                            Throwable cause =
                                    failure.getCause() == null
                                        ? failure
                                        : failure.getCause();

                            error(cause.getMessage());

                            return;
                        }

                        generated.set(List.copyOf(items));

                        preview.setItems(items);
                        preview.setVisible(true);

                        apply.setEnabled(!items.isEmpty());

                        success(
                                "AI menghasilkan "
                                + items.size()
                                + " transaksi. Periksa preview."
                        );

                    } finally {

                        loading.setVisible(false);

                        generate.setEnabled(true);
                        cancel.setEnabled(true);
                        prompt.setReadOnly(false);

                        ui.setPollInterval(previousPollInterval);
                    }
                });
            });
        });

        apply.addClickListener(event -> {

            List<AiGeneratedTransaction> items =
                    generated.get();

            if (items.isEmpty()) {

                error("Belum ada hasil AI");
                return;
            }

            apply.setEnabled(false);
            generate.setEnabled(false);
            cancel.setEnabled(false);

            try {

                financeAiUseCase.saveGeneratedTransactions(
                        currentUserId(),
                        items
                );

                success(
                        presenter.successGenerate(items.size())
                );

                dialog.close();

                refreshAll();

            } catch (Exception ex) {

                error(
                    "Penyimpanan gagal. Periksa daftar transaksi "
                    + "sebelum mencoba kembali: "
                    + ex.getMessage()
                );

                generated.set(List.of());
                dialog.close();
                refreshAll();
            }
        });

        VerticalLayout body = new VerticalLayout(
                new Paragraph(
                    "Masukkan catatan keuangan. "
                    + "Hasil AI harus diperiksa sebelum disimpan."
                ),
                prompt,
                generate,
                loading,
                preview
        );

        setupDialog(
                dialog,
                "Generate AI Cashflow",
                body,
                cancel,
                apply
        );

        dialog.setWidth("700px");

        dialog.open();
    }

    private void setupDialog(
            Dialog dialog,
            String title,
            Component body,
            Button... buttons
    ) {

        dialog.setWidth("520px");
        dialog.setMaxWidth("95vw");

        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        Span label = new Span(title);

        label.getStyle()
                .set("font-weight", "700")
                .set("color", "#FFFFFF");

        Div header = new Div(label);

        header.getStyle()
                .set("background", "#2563EB")
                .set("padding", "16px");

        Div content = new Div(body);

        content.getStyle()
                .set("padding", "12px");

        HorizontalLayout footer =
                new HorizontalLayout(buttons);

        footer.setWidthFull();

        footer.setJustifyContentMode(
                JustifyContentMode.END
        );

        footer.getStyle()
                .set("background", "#F3F4F6")
                .set("padding", "12px")
                .set("box-sizing", "border-box");

        for (Button button : buttons) {

            button.getStyle().set("cursor", "pointer");
        }

        Div shell = new Div(header, content, footer);

        shell.setWidthFull();

        shell.getStyle()
                .set("overflow", "hidden")
                .set("border-radius", "10px");

        dialog.add(shell);
    }

    private void success(String message) {

        Notification notification = Notification.show(
                message,
                3000,
                Notification.Position.BOTTOM_START
        );

        notification.addThemeVariants(
                NotificationVariant.LUMO_SUCCESS
        );
    }

    private void error(String message) {

        Notification notification = Notification.show(
                message == null
                    ? "Terjadi kesalahan"
                    : message,
                5000,
                Notification.Position.BOTTOM_START
        );

        notification.addThemeVariants(
                NotificationVariant.LUMO_ERROR
        );
    }
}