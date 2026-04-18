package dev.teamwin.contafacil.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "compras_cartao")
public class ComprasCartaoDomain {

        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @Column(nullable = false, precision = 19, scale = 2)
        private BigDecimal valor;

        @Column(nullable = false)
        private LocalDateTime dataCompra;

        @Column(nullable = false, length = 120)
        private String estabelecimento;

        @Column(nullable = false, length = 4)
        private String ultimos4Digitos;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 40)
        private CategoriaEstabelecimento categoria;

    @PrePersist
    void onCreate() {
        if (dataCompra == null) {
            dataCompra = LocalDateTime.now();
        }
    }


        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "fatura_id", nullable = false)
        private FaturaDomain fatura;
}
