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
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryQueryImpl implements UserRepositoryQuery {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<DeliveryHistoryResponseDto> findDeliveryHistoryByUserId(Long userId) {
        QUser user = QUser.user;
        QOrder order = QOrder.order;
        QProduct product = QProduct.product;

        OrderSpecifier<?> orderSpecifier = new OrderSpecifier<>(Order.DESC, order.createdAt);

        return jpaQueryFactory.select(
                Projections.constructor(DeliveryHistoryResponseDto.class,
                        order.orderStatus,
                        product.name,
                        user.address
                )
        ).from(user)
                .where(user.id.eq(userId))
                .leftJoin(order).on(user.id.eq(order.user.id)).fetchJoin()
                .leftJoin(product).on(order.product.id.eq(product.id)).fetchJoin()
                .orderBy(orderSpecifier)
                .limit(5)
                .fetch();


    }

}
