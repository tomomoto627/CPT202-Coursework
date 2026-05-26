package org.example.coursework3.controller;

import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.BookingStatus;
import org.example.coursework3.entity.Slot;
import org.example.coursework3.entity.SpecialistStatus;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.service.SlotInfoService;
import org.example.coursework3.service.SpecialistsInfoService;
import org.example.coursework3.vo.SlotVo;
import org.example.coursework3.vo.SpecialistsDetailVo;
import org.example.coursework3.vo.SpecialistsExpertiseBriefVo;
import org.example.coursework3.vo.SpecialistsPageVo;
import org.example.coursework3.vo.SpecialistsVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpecialistsController.class)
@AutoConfigureMockMvc(addFilters = false)
class SpecialistsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpecialistsInfoService specialistsInfoService;

    @MockBean
    private SlotInfoService slotInfoService;

    @Test
    void getSpecialists_returnsPagedResult() throws Exception {
        SpecialistsVo specialistVo = new SpecialistsVo(
                "u3", "Dr.Smith", SpecialistStatus.Active,
                List.of("e1", "e2"), List.of("Mental Health", "Stress Management"), BigDecimal.valueOf(100)
        );
        SpecialistsPageVo pageVo = new SpecialistsPageVo(
                List.of(specialistVo), 1L, 1, 10
        );

        when(specialistsInfoService.getSpecialists(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(pageVo);

        mockMvc.perform(get("/specialists")
                        .param("expertiseId", "e1")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].id").value("u3"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getSpecialists_usesDefaultPageParamsWhenOmitted() throws Exception {
        SpecialistsPageVo emptyPage = new SpecialistsPageVo(List.of(), 0L, 1, 10);
        when(specialistsInfoService.getSpecialists(isNull(), isNull(), isNull(), isNull(), eq(1), eq(10))).thenReturn(emptyPage);

        mockMvc.perform(get("/specialists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void getSpecialistDetail_returnsDetail() throws Exception {
        SpecialistsExpertiseBriefVo expertiseVo = new SpecialistsExpertiseBriefVo("e1", "Cardiology");
        SpecialistsDetailVo detailVo = new SpecialistsDetailVo(
                "u3", "Dr.Smith", "Mental Health",
                List.of(expertiseVo), BigDecimal.valueOf(100)
        );

        when(specialistsInfoService.getSpecialistDetail(eq("u3")))
                .thenReturn(detailVo);

        mockMvc.perform(get("/specialists/u3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("u3"));
    }

    @Test
    void getSpecialistSlots_returnsUnauthorizedWhenServiceThrowsMsgException() throws Exception {
        when(slotInfoService.getSpecialistSlots(eq("u3"), eq("2026-05-20"), isNull(), isNull()))
                .thenThrow(new MsgException("No permission to view this specialist's schedule."));

        mockMvc.perform(get("/specialists/u3/slots")
                        .param("date", "2026-05-20"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No permission to view this specialist's schedule."));
    }

    @Test
    void getSpecialistSlots_returnsSlots() throws Exception {
        Slot slot = new Slot();
        slot.setId("s1");
        slot.setStartTime(LocalDateTime.of(2026, 5, 20, 9, 0));
        slot.setEndTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        slot.setAvailable(true);
        slot.setAmount(BigDecimal.valueOf(100));
        slot.setCurrency("USD");
        slot.setDuration(60);
        slot.setType("online");
        slot.setDetail("1 hour online session");

        Booking booking = new Booking();
        booking.setId("b1");
        booking.setStatus(BookingStatus.Confirmed);

        SlotVo slotVo = SlotVo.fromSlot(slot, booking, "Alice");
        List<SlotVo> slotList = List.of(slotVo);

        when(slotInfoService.getSpecialistSlots(eq("u3"), any(), any(), any()))
                .thenReturn(slotList);

        mockMvc.perform(get("/specialists/u3/slots")
                        .param("date", "2026-05-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slotId").value("s1"))
                .andExpect(jsonPath("$.data[0].start").value("2026-05-20 09:00:00"))
                .andExpect(jsonPath("$.data[0].status").value("Confirmed"));
    }

    @Test
    void getSpecialistSlots_returnsEmptyListWhenNoSlots() throws Exception {
        when(slotInfoService.getSpecialistSlots(eq("u3"), eq("2026-05-21"), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/specialists/u3/slots")
                        .param("date", "2026-05-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
