package dev.teamwin.contafacil.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cartoes")
public class CartaoDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal limiteTotal;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal limiteUtilizado = BigDecimal.ZERO;

    @Transient
    public BigDecimal getLimiteDisponivel() {
        return limiteTotal.subtract(limiteUtilizado);
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCartao status;

    @Column(nullable = false, unique = true, length = 19)
    private String numeroCartao;

    @Column(nullable = false, length = 4)
    private String cvv;

    @Column(nullable = false)
    private LocalDateTime dataValidade;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (dataValidade == null) {
            dataValidade = LocalDateTime.now().plusYears(5);
        }
        if (limiteTotal == null) {
            limiteTotal = BigDecimal.ZERO.setScale(2);
        }
        if (limiteUtilizado == null) {
            limiteUtilizado = BigDecimal.ZERO.setScale(2);
        }
        if (this.status == null) {
            this.status = StatusCartao.INATIVO;
        }
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private ContaDomain conta;


    @OneToMany(mappedBy = "cartao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FaturaDomain> faturas = new ArrayList<>();

}
