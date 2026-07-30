package com.dividendbot.news.promotion.channel;

import com.dividendbot.news.promotion.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channels")
public class ChannelController {

    private final ChannelCatalog catalog;

    public ChannelController(ChannelCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<Channel> list() {
        return catalog.all();
    }

    @GetMapping("/{channelId}")
    public Channel get(@PathVariable String channelId) {
        return catalog.findById(channelId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "CHANNEL_NOT_FOUND",
                        "채널을 찾을 수 없습니다."
                ));
    }
}

