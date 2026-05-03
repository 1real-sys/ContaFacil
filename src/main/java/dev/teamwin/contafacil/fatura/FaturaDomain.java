package dev.teamwin.contafacil.fatura;

import dev.teamwin.contafacil.cartao.CartaoDomain;
import dev.teamwin.contafacil.comprasCartao.ComprasCartaoDomain;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "faturas",
        uniqueConstraints = @UniqueConstraint(name = "uk_fatura_cartao_ano_mes", columnNames = {"cartao_id", "ano", "mes"})
)
public class FaturaDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer ano;

    @NotNull
    @Min(1)
    @Max(12)
    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private LocalDate dataFechamento;

    @Column(nullable = false)
    private LocalDate dataVencimento;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorPago = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusFatura status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cartao_id", nullable = false)
    private CartaoDomain cartao;

    @OneToMany(mappedBy = "fatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComprasCartaoDomain> compras = new ArrayList<>();

    @Transient
    public BigDecimal getValorPendente() {
        return valorTotal.subtract(valorPago);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (valorTotal == null) {
            valorTotal = BigDecimal.ZERO.setScale(2);
        }
        if (valorPago == null) {
            valorPago = BigDecimal.ZERO.setScale(2);
        }
        if (status == null) {
            status = StatusFatura.ABERTA;
        }
    }
}
