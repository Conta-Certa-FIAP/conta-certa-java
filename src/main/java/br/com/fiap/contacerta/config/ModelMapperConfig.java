package br.com.fiap.contacerta.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ModelMapper copia os campos de um objeto para outro quando os NOMES são iguais
 * (ex.: DividaRequest.credor -> Divida.credor). É ele que converte DTO <-> model.
 *
 * Usamos a estratégia STRICT: só copia quando o nome bate exatamente.
 * Sem isso ele "adivinha" por semelhança e poderia, por exemplo, jogar o valor de
 * "contaId" dentro de "id". Campos calculados (cpfMascarado, tipoDescricao...) são
 * preenchidos à mão nos services.
 */
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper;
    }
}
