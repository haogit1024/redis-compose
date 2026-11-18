package com.czh.rediscompose.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

fun appShapes(dimens: AppDimens): Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(dimens.radiusXs),
        small = RoundedCornerShape(dimens.radiusSm),
        medium = RoundedCornerShape(dimens.radiusMd),
        large = RoundedCornerShape(dimens.radiusLg),
        extraLarge = RoundedCornerShape(dimens.radiusXl),
    )
