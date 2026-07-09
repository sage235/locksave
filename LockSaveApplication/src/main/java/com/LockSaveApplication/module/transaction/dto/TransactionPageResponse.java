// module/transaction/dto/TransactionPageResponse.java

package com.LockSaveApplication.module.transaction.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TransactionPageResponse {
    private List<TransactionResponse> transactions;
    private int                       page;
    private int                       size;
    private long                      totalElements;
    private int                       totalPages;
    private boolean                   last;
}