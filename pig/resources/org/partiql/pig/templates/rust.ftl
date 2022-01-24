[#ftl output_format="plainText"]
[#assign enable_bulider=false/]

[#macro tuple_elements tuple includeVis indentCount]
[@indent count=indentCount]
[#list tuple.elements as element]
    [#if includeVis]pub [/#if]${element.identifier.snake} : ${element.structTypeName},
    // TODO: return here: make this support optional and variadic elements.
[/#list]
    // TODO: metas
[/@indent][/#macro]
[#--end tuple_elements --]
[#macro builder_fn tuple indentCount]
[@indent count=indentCount]
fn ${tuple.tag.snake}(&self,
[#list tuple.elements as element]
    ${element.identifier.snake}: ${element.builderParameterTypeName}[#sep],[/#sep]
[/#list]
    // TODO: metas
) -> [#if tuple.memberOfType??]${tuple.memberOfType.pascal}[#else]${tuple.tag.pascal}[/#if] {
    [#if tuple.memberOfType??]${tuple.memberOfType.pascal}::[/#if]${tuple.tag.pascal} {
        [#list tuple.elements as element]
        ${element.identifier.snake}: Box::new(${element.identifier.snake})[#sep],[/#sep]
        [/#list]
    }
}
[/@indent]
[#-- end builder_fn--]

[/#macro]
[#list domains as domain]
//noinspection DuplicatedCode
#[allow(dead_code)]
#[allow(unused_imports)]
pub mod ${domain.name.snake} {
    use crate::primitives::{SymbolPrimitive, IonPrimitive, IntPrimitive};
    use crate::primitives::{AsIonPrimitive, AsSymbolPrimitive, AsIntPrimitive};

    //
    // Product Types
    //

[#if domain.tuples?size == 0]
    // Note: this domain has no product types.
[#else]
    [#list domain.tuples as tuple]
    #[derive(Debug)]
    pub struct ${tuple.tag.pascal} {
        [@tuple_elements tuple=tuple includeVis=true indentCount=4/][#lt]
    }
    [/#list]
[/#if]
    //
    // Sum Types
    //
[#if domain.sums?size == 0]
    // Note: this domain has no sum types.
[#else]
    [#list domain.sums as sum]

    #[derive(Debug)]
    pub enum ${sum.name.pascal} {
    [#list sum.variants as tuple]
        ${tuple.tag.pascal} {
           [@tuple_elements tuple=tuple includeVis=false indentCount=8/][#lt]
        }[#sep],[/#sep]
    [/#list]
    }

    [/#list]

    [#if enable_bulider]
    //
    // Builder
    //
    pub trait Builder {
        // Product constructors
        [#list domain.tuples as tuple]
        [@builder_fn tuple=tuple indentCount=8/]
        [/#list]

        [#list domain.sums as s]
        // Sum type: ${s.name.pascal}
        [#list s.variants as tuple]
        [@builder_fn tuple=tuple indentCount=8/]
        [/#list]
        [/#list]
    }

    struct BuilderInstance;
    impl Builder for BuilderInstance {}

    pub fn build<T>(block: fn(&dyn Builder) -> T) -> T {
        return block(&BuilderInstance)
    }
[/#if] [#-- end if enable_bulider --]
[/#if] [#-- end if domain.sums?size == 0 --]
}
[/#list]
