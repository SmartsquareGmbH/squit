<template>
  <Transition @enter="onEnter" @after-enter="onAfterEnter" @leave="onLeave" @after-leave="onAfterLeave">
    <slot />
  </Transition>
</template>

<script setup lang="ts">
function onEnter(el: Element) {
  const element = el as HTMLElement
  element.style.height = "0"
  element.style.overflow = "hidden"

  requestAnimationFrame(() => {
    element.style.transition = "height 0.2s ease"
    element.style.height = `${element.scrollHeight}px`
  })
}

function onAfterEnter(el: Element) {
  const element = el as HTMLElement
  element.style.height = ""
  element.style.overflow = ""
  element.style.transition = ""
}

function onLeave(el: Element) {
  const element = el as HTMLElement
  element.style.height = `${element.scrollHeight}px`
  element.style.overflow = "hidden"

  // oxlint-disable-next-line no-unused-expressions -- Force reflow.
  element.offsetHeight

  element.style.transition = "height 0.2s ease"
  element.style.height = "0"
}

function onAfterLeave(el: Element) {
  const element = el as HTMLElement
  element.style.height = ""
  element.style.overflow = ""
  element.style.transition = ""
}
</script>
