
package dev.teamwin.contafacil.comprasCartao;


import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ajustes_compra")
public class AjusteCompraDomain {


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorCreditado;


    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorAbatido;

    @Column(nullable = false)
    private LocalDateTime dataAjuste;

    @PrePersist
    void onCreate() {
        if (dataAjuste == null) {
            dataAjuste = LocalDateTime.now();
        }
    }


    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false, unique = true)
    private ComprasCartaoDomain compra;





}
