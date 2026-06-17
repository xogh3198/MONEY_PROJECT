package com.dividendbot.engine.domain.repository;

import com.dividendbot.engine.domain.entity.DividendInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DividendInfoRepository extends JpaRepository<DividendInfo, UUID> {

    List<DividendInfo> findByStockCodeAndYear(String stockCode, int year);

    @Query("SELECT d FROM DividendInfo d WHERE d.stockCode IN :stockCodes AND d.year = :year")
    List<DividendInfo> findByStockCodesAndYear(
            @Param("stockCodes") List<String> stockCodes,
            @Param("year") int year);

    @Query("SELECT d FROM DividendInfo d WHERE d.exDividendDate BETWEEN :start AND :end")
    List<DividendInfo> findByExDividendDateBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
