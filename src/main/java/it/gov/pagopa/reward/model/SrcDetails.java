package it.gov.pagopa.reward.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SrcDetails {
    private String srcType;
    private String srcServer;
    private String srcTopic;
}