// K2 typed-schema test. Schemas at
// `data/supreme_crafting/kubejs/recipe_schema/supreme_{shaped,shapeless}.json`
// inherit from KubeJS's built-in `shaped`/`shapeless` schemas — our recipe
// codec accepts the same JSON shape as vanilla, just with larger size limits.
//
// Both recipe types now appear under `event.recipes.supreme_crafting.*` with
// constructor signatures matching the parent schemas:
//   supreme_shaped(result, pattern, key)
//   supreme_shapeless(result, ingredients)
//
// After editing this file: /reload in dev client; KubeJS prints the recipes
// it added; they show up in EMI/JEI under the Supreme Crafting category.

ServerEvents.recipes(event => {
    // Disabled — kept as reference for the typed-schema API. Uncomment to test.
    /*
    // Shapeless: 4 dirt -> 1 emerald.
    event.recipes.supreme_crafting.supreme_shapeless(
        'minecraft:emerald',
        ['minecraft:dirt', 'minecraft:dirt', 'minecraft:dirt', 'minecraft:dirt']
    ).id('supreme_crafting:kubejs_test_dirt_to_emerald')

    // Shaped: 9 cobblestone in 3x3 -> 1 stone block.
    event.recipes.supreme_crafting.supreme_shaped(
        'minecraft:stone',
        ['CCC', 'CCC', 'CCC'],
        { C: 'minecraft:cobblestone' }
    ).id('supreme_crafting:kubejs_test_cobble_to_stone')
    */
})
