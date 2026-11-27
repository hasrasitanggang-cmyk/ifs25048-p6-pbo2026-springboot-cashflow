package org.delcom.ifs25048springbootcashflow.framework.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.delcom.ifs25048springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25048springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25048springbootcashflow.domain.entity.User;
import org.delcom.ifs25048springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25048springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25048springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25048springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25048springbootcashflow.usecase.UserUseCase;

@Route(value = "user-transactions", layout = MainLayout.class)
@PageTitle("Transaksi Pengguna")
public class UserTransactionsView
        extends VerticalLayout
        implements HasUrlParameter<Integer>,
                   BeforeEnterObserver {

    private final UserUseCase userUseCase;
    private final FinanceUseCase financeUseCase;
    private final FinancePresenter presenter;
    private final IUserRepository userRepository;

    private final H1 title =
            new H1("Transaksi Pengguna");

    private final Paragraph summary =
            new Paragraph();

    private final Grid<Transaction> grid =
            new Grid<>(Transaction.class, false);

    private Integer targetUserId;

    public UserTransactionsView(
            UserUseCase userUseCase,
            FinanceUseCase financeUseCase,
            FinancePresenter presenter,
            IUserRepository userRepository
    ) {

        this.userUseCase = userUseCase;
        this.financeUseCase = financeUseCase;
        this.presenter = presenter;
        this.userRepository = userRepository;

        setSizeFull();
        setPadding(true);

        getStyle().set("background", "#F3F4F6");

        configureGrid();

        Div card = PageCard.wrap(
                title,
                new Span("Mode read-only"),
                summary,
                grid
        );

        add(card);

        expand(card);
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
                .setHeader("ID");

        grid.addColumn(Transaction::getDescription)
                .setHeader("Keterangan")
                .setFlexGrow(2);

        grid.addColumn(transaction ->
                presenter.formatAmount(
                        transaction.getAmount()
                )
        ).setHeader("Jumlah");

        grid.addColumn(transaction ->
                transaction.getType().label()
        ).setHeader("Tipe");

        grid.addColumn(transaction ->
                presenter.formatDateTime(
                        transaction.getCreatedAt()
                )
        ).setHeader("Waktu");

        grid.setSizeFull();

        GridStyles.apply(grid);
    }

    @Override
    public void setParameter(
            BeforeEvent event,
            @OptionalParameter Integer userId
    ) {

        targetUserId = userId;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        User current = AuthSession
                .currentUser(userRepository)
                .orElse(null);

        if (current == null) {

            event.forwardTo(LoginView.class);
            return;
        }

        if (!current.isAdmin()) {

            event.forwardTo(FinanceView.class);
            return;
        }

        if (targetUserId == null || targetUserId <= 0) {

            event.forwardTo(UsersView.class);
            return;
        }

        try {

            User owner =
                    userUseCase.getUser(targetUserId);

            title.setText(
                    "Transaksi milik "
                    + owner.getFullName()
                    + " (@" + owner.getUsername() + ")"
            );

            grid.setItems(
                    financeUseCase.getTransactionsByUser(
                            targetUserId
                    )
            );

            summary.setText(
                    "Pemasukan: "
                    + presenter.formatAmount(
                        financeUseCase.getTotalIncome(targetUserId)
                    )
                    + " | Pengeluaran: "
                    + presenter.formatAmount(
                        financeUseCase.getTotalExpense(targetUserId)
                    )
                    + " | Saldo: "
                    + presenter.formatAmount(
                        financeUseCase.getBalance(targetUserId)
                    )
            );

        } catch (EntityNotFoundException ex) {

            event.forwardTo(UsersView.class);
        }
    }
}