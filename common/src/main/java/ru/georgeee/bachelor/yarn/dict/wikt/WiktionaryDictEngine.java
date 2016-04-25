package ru.georgeee.bachelor.yarn.dict.wikt;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.georgeee.bachelor.yarn.common.IdValueMapping;
import ru.georgeee.bachelor.yarn.core.POS;
import ru.georgeee.bachelor.yarn.dict.Dict;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WiktionaryDictEngine {
    private final JdbcTemplate jdbcTemplate;
    private final IdValueMapping<WiktLang> langMapping;
    private final IdValueMapping<POS> posMapping;

    private static final Map<String, POS> POS_NAME_MAPPING;

    static {
        Map<String, POS> map = new HashMap<>();
        map.put("adverb", POS.ADVERB);
        map.put("participle", POS.ADJECTIVE);
        map.put("adjective", POS.ADJECTIVE);
        map.put("verb", POS.VERB);
        map.put("noun", POS.NOUN);
        POS_NAME_MAPPING = Collections.unmodifiableMap(map);
    }

    public WiktionaryDictEngine(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        langMapping = retrieveMappingByCode(jdbcTemplate, "SELECT id, code FROM lang", WiktLang::getByCode);
        posMapping = retrieveMappingByCode(jdbcTemplate, "SELECT id, name as code FROM part_of_speech", POS_NAME_MAPPING::get);
    }

    private static <T> IdValueMapping<T> retrieveMappingByCode(JdbcTemplate jdbcTemplate, String sql, Function<String, T> mapper) {
        Map<T, Integer> idByCodeMap = new HashMap<>();
        Map<Integer, T> codeByIdMap = new HashMap<>();
        jdbcTemplate
                .queryForList(sql).stream()
                .forEach(r -> {
                    T value = mapper.apply((String) r.get("code"));
                    if (value != null) {
                        Integer id = (Integer) r.get("id");
                        idByCodeMap.put(value, id);
                        codeByIdMap.put(id, value);
                    }
                });
        return new IdValueMapping<>(idByCodeMap, codeByIdMap);
    }

    private static final String SQL_LOOKUP_BASE = "SELECT p.id as page_id,\n" +
            "\t   lp.lang_id as src_lang_id,\n" +
            "       lp.pos_id,\n" +
            "\t   t.id as trans_id, t.meaning_summary,\n" +
            "       te.lang_id as dest_lang_id, wt.text\n" +
            "FROM page p\n" +
            "JOIN lang_pos lp ON lp.page_id = p.id\n" +
            "JOIN translation t ON lp.id = t.lang_pos_id\n" +
            "JOIN translation_entry te ON t.id = te.translation_id\n" +
            "JOIN wiki_text wt ON te.wiki_text_id = wt.id\n";

    private static final String SQL_LOOKUP_COND_SRC_LANG = "lp.lang_id = ?";
    private static final String SQL_LOOKUP_COND_DEST_LANG = "te.lang_id = ?";
    private static final String SQL_LOOKUP_COND_QUERY = "p.page_title = ?";
    private static final String SQL_LOOKUP_COND_ALL = SQL_LOOKUP_BASE + " WHERE "
            + StringUtils.join(new String[]{SQL_LOOKUP_COND_QUERY, SQL_LOOKUP_COND_SRC_LANG, SQL_LOOKUP_COND_DEST_LANG}, " AND ");

    public List<Dict.Translation> lookup(String word, WiktLang from, WiktLang to) {
        Map<Long, List<RowTranslationResult>> byTransId = new HashMap<>();
        jdbcTemplate.queryForList(SQL_LOOKUP_COND_ALL, word, langMapping.getByCode(from), langMapping.getByCode(to))
                .stream().forEach(m -> {
            RowTranslationResult res = new RowTranslationResult();
            res.pageId = (Long) m.get("page_id");
            res.srcLangId = (Integer) m.get("src_lang_id");
            res.posId = (Integer) m.get("pos_id");
            res.transId = (Long) m.get("trans_id");
            res.meaningSummary = (String) m.get("meaning_summary");
            res.destLangId = (Integer) m.get("dest_lang_id");
            res.text = (String) m.get("text");
            byTransId.computeIfAbsent(res.transId, i -> new ArrayList<>()).add(res);
        });
        return byTransId.values().stream().map(ls -> {
            RowTranslationResult fst = ls.get(0);
            List<String> words = ls.stream().map(RowTranslationResult::getText).collect(Collectors.toList());
            return new Dict.Translation(words, posMapping.getById(fst.posId), fst.meaningSummary);
        }).collect(Collectors.toList());
    }

    private static class RowTranslationResult {
        long pageId;
        Integer srcLangId;
        Integer posId;
        long transId;
        String meaningSummary;
        Integer destLangId;
        @Getter
        String text;
    }

}
