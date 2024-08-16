package com.example.livealone.user.repository;

import com.example.livealone.delivery.dto.DeliveryHistoryResponseDto;
import com.example.livealone.order.entity.QOrder;
import com.example.livealone.product.entity.QProduct;
import com.example.livealone.user.entity.QUser;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryQueryImpl implements UserRepositoryQuery {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<DeliveryHistoryResponseDto> findDeliveryHistoryByUserId(Long userId, int page) {
        QUser user = QUser.user;
        QOrder order = QOrder.order;
        QProduct product = QProduct.product;

        PageRequest pageRequest = PageRequest.of(page,5);
        OrderSpecifier<?> orderSpecifier = new OrderSpecifier<>(Order.DESC, order.id);

        long total = jpaQueryFactory.select(order.count())
            .from(user)
            .where(user.id.eq(userId))
            .innerJoin(order).on(user.id.eq(order.user.id))
            .innerJoin(product).on(order.product.id.eq(product.id))
            .fetchOne();

        List<DeliveryHistoryResponseDto> content = jpaQueryFactory.select(
                Projections.constructor(DeliveryHistoryResponseDto.class,
                    order.orderStatus,
                    product.name,
                    user.address
                )
            ).from(user)
            .where(user.id.eq(userId))
            .innerJoin(order).on(user.id.eq(order.user.id)).fetchJoin()
            .innerJoin(product).on(order.product.id.eq(product.id)).fetchJoin()
            .offset(pageRequest.getOffset())
            .limit(pageRequest.getPageSize())
            .orderBy(orderSpecifier)
            .fetch();

        return new PageImpl<>(content, pageRequest, total);

    }

}
