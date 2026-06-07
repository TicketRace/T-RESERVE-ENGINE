package com.treserve.booking.mapper;

import com.treserve.booking.dto.LockResponse;
import com.treserve.booking.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(source = "id", target = "lockId")
    @Mapping(source = "lockExpiresAt", target = "expiresAt")
    LockResponse toLockResponse(Ticket ticket);

    List<LockResponse> toLockResponseList(List<Ticket> tickets);
}