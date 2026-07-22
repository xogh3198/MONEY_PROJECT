package com.dividendbot.news.service.engagement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RobotsPolicyServiceTest {

    @Test
    void blocksAllWhenWildcardDisallowsRoot() {
        String robots = "User-agent: *\nDisallow: /\n";
        assertThat(RobotsPolicyService.isAllowed(robots, "InvestBoardBot", "/article/1")).isFalse();
    }

    @Test
    void longestAllowRuleWins() {
        String robots = """
                User-agent: *
                Disallow: /
                Allow: /news/public/
                """;
        assertThat(RobotsPolicyService.isAllowed(robots, "InvestBoardBot", "/news/public/123")).isTrue();
        assertThat(RobotsPolicyService.isAllowed(robots, "InvestBoardBot", "/news/private/123")).isFalse();
    }

    @Test
    void botSpecificGroupOverridesWildcardGroup() {
        String robots = """
                User-agent: InvestBoardBot
                Allow: /

                User-agent: *
                Disallow: /
                """;
        assertThat(RobotsPolicyService.isAllowed(robots, "InvestBoardBot", "/article")).isTrue();
    }

    @Test
    void emptySpecificGroupDoesNotMergeWithWildcardGroup() {
        String robots = """
                User-agent: InvestBoardBot

                User-agent: *
                Disallow: /
                """;
        assertThat(RobotsPolicyService.isAllowed(robots, "InvestBoardBot", "/article")).isTrue();
    }

    @Test
    void supportsWildcardAndEndAnchor() {
        String robots = "User-agent: *\nDisallow: /*?preview=true$\n";
        assertThat(RobotsPolicyService.isAllowed(robots, "InvestBoardBot", "/story?preview=true")).isFalse();
        assertThat(RobotsPolicyService.isAllowed(robots, "InvestBoardBot", "/story?preview=true&x=1")).isTrue();
    }
}
