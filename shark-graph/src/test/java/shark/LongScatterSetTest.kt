package shark

import org.assertj.core.api.Assertions
import org.junit.Test
import shark.LongScatterSetAssertion.Companion.assertThat
import shark.internal.hppc.HHPC
import shark.internal.hppc.LongScatterSet

class LongScatterSetTest {

  @Test fun `verify LongScatterSet#add operation works correctly`() {
    val set = LongScatterSet()
    verifyAddOperation(set)
  }

  @Test fun `verify LongScatterSet#remove operation works correctly`() {
    val set = LongScatterSet()
    verifyRemoveOperation(set)
  }

  @Test fun `verify LongScatterSet#release operation works correctly`() {
    val set = LongScatterSet()
    set.add(42)
    set.release()

    assertThat(set)
        .isEmpty()
        .doesNotContain(42)
  }

  @Test fun `verify LongScatterSet#ensureCapacity does not break other operations`() {
    val setForAdditionOperationCheck = LongScatterSet().apply { ensureCapacity(10) }
    val setForRemoveOperationCheck = LongScatterSet().apply { ensureCapacity(10) }
    val setForTwoAdditionsCheck = LongScatterSet().apply {
      add(42)
      add(10)
      ensureCapacity(100)
    }

    // Ensure capacity before adding/removing elements
    verifyAddOperation(setForAdditionOperationCheck)
    verifyRemoveOperation(setForRemoveOperationCheck)

    // Ensure capacity after adding/removing elements

    assertThat(setForTwoAdditionsCheck)
        .contains(10, 42)
        .hasSize(2)
  }

  private fun verifyRemoveOperation(set: LongScatterSet) {
    val testValues = listOf(42, 0, Long.MIN_VALUE, Long.MAX_VALUE, -1)

    // First, check removing unique values on empty list
    testValues.forEach { value ->
      set.remove(value)
      assertThat(set)
          .isEmpty()
          .doesNotContain(value)
    }

    // Check removing unique values on filled list
    testValues.forEach { set += it }

    testValues.forEachIndexed { index: Int, value: Long ->
      // Values is in the set at first
      assertThat(set).contains(value)

      set.remove(value)

      // Value is removed, size decreased, other elements are still there
      assertThat(set)
          .doesNotContain(value)
          .hasSize(testValues.size - index - 1)
      for (i in index + 1 until testValues.size) {
        assertThat(set).contains(testValues[i])
      }
    }

    // Check removing same element twice
    set.add(42)
    set.remove(42)
    set.remove(42)
    assertThat(set)
        .isEmpty()
        .doesNotContain(42)

    // Check removing elements with same hash. Remove in reverse order than inserting
    set += 11
    set += 14723950898

    set.remove(14723950898)
    set.remove(11)
    assertThat(set).doesNotContain(11, 14723950898)
  }

  private fun verifyAddOperation(set: LongScatterSet) {
    // First, check adding unique values + having matching hashKeys
    // Values 11 and 14723950898 give same hash when using HHPC.mixPhi()
    Assertions.assertThat(HHPC.mixPhi(14723950898)).isEqualTo(HHPC.mixPhi(11))

    val testValues = listOf(42, 0, Long.MIN_VALUE, Long.MAX_VALUE, -1, 11, 14723950898)

    testValues.forEachIndexed { index: Int, value: Long ->
      // Values is not yet in the set
      assertThat(set)
          .doesNotContain(value)
          .hasSize(index)

      set.add(value)

      // Size increases by one, element and all previous elements should be in the set
      assertThat(set).hasSize(index + 1)
      for (i in 0 until index + 1) {
        assertThat(set).contains(testValues[i])
      }
    }

    // Check the += operator
    set += 30
    assertThat(set).contains(30)

    // Check adding element that was already there
    val currentSize = set.size()
    set.add(testValues.first())
    assertThat(set).hasSize(currentSize)
  }
}
