/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.json

enum class ElementType {
  NULL,
  OBJECT,
  ARRAY,
  STRING,
  NUMBER,
  BOOLEAN
}

/**
 * Class representing single JSON element.
 * Can be [JsonPrimitive], [JsonArray] or [JsonObject].
 *
 * [JsonElement.toString] properly prints JSON tree as valid JSON, taking into
 * account quoted values and primitives
 */
sealed class JsonElement {

  /**
   * Convenience method to get current element as [JsonPrimitive]
   * @throws JsonElementTypeMismatchException is current element is not a [JsonPrimitive]
   */
  open val primitive: JsonPrimitive
    get() = error("JsonLiteral")

  /**
   * Convenience method to get current element as [JsonObject]
   * @throws JsonElementTypeMismatchException is current element is not a [JsonObject]
   */
  open val jsonObject: JsonObject
    get() = error("JsonObject")

  /**
   * Convenience method to get current element as [JsonArray]
   * @throws JsonElementTypeMismatchException is current element is not a [JsonArray]
   */
  open val jsonArray: JsonArray
    get() = error("JsonArray")

  /**
   * Convenience method to get current element as [JsonNull]
   * @throws JsonElementTypeMismatchException is current element is not a [JsonNull]
   */
  open val jsonNull: JsonNull
    get() = error("JsonPrimitive")

  open val numberOrNull: Number?
    get() = error("JsonPrimitive")

  open val booleanOrNull: Boolean?
    get() = error("JsonPrimitive")

  open val stringOrNull: String?
    get() = error("JsonPrimitive")

  @Suppress("LeakingThis")
  open val type: ElementType by lazy {
    when (this) {
      is JsonObject -> ElementType.OBJECT
      is JsonArray -> ElementType.ARRAY
      is JsonPrimitive -> when (literal) {
        null -> ElementType.NULL
        is String -> ElementType.STRING
        is Number -> ElementType.NUMBER
        is Boolean -> ElementType.BOOLEAN
        else -> kotlin.error("Cant determine type for ${this}")
      }
    }
  }

  /**
   * Checks whether current element is [JsonNull]
   */
  val isNull: Boolean
    get() = this === JsonNull

  private fun error(element: String): Nothing =
      throw JsonElementTypeMismatchException(this::class.toString(), element)
}

/**
 * Class representing JSON primitive value. Can be either [JsonLiteral] or [JsonNull].
 */
sealed class JsonPrimitive(val literal: Any?) : JsonElement() {
  /**
   * Content of given element without quotes. For [JsonNull] this methods returns `"null"`
   */
  abstract val content: String

  /**
   * Content of the given element without quotes or `null` if current element is [JsonNull]
   */
  abstract val contentOrNull: String?

  @Suppress("LeakingThis")
  final override val primitive: JsonPrimitive = this

  /**
   * Returns content of current element as int
   * @throws NumberFormatException if current element is not a valid representation of number
   */
  val int: Int get() = number.toInt()

  /**
   * Returns content of current element as int or `null` if current element is not a valid representation of number
   **/
  val intOrNull: Int? get() = numberOrNull?.toInt()

  /**
   * Returns content of current element as long
   * @throws NumberFormatException if current element is not a valid representation of number
   */
  val long: Long get() = number.toLong()

  /**
   * Returns content of current element as long or `null` if current element is not a valid representation of number
   */
  val longOrNull: Long? get() = numberOrNull?.toLong()

  /**
   * Returns content of current element as double
   * @throws NumberFormatException if current element is not a valid representation of number
   */
  val double: Double get() = number.toDouble()

  /**
   * Returns content of current element as double or `null` if current element is not a valid representation of number
   */
  val doubleOrNull: Double? get() = numberOrNull?.toDouble()

  /**
   * Returns content of current element as float
   * @throws NumberFormatException if current element is not a valid representation of number
   */
  val float: Float get() = number.toFloat()

  /**
   * Returns content of current element as float or `null` if current element is not a valid representation of number
   */
  val floatOrNull: Float? get() = numberOrNull?.toFloat()

  /**
   * Returns content of current element as boolean
   * @throws IllegalStateException if current element doesn't represent boolean
   */
  val boolean: Boolean get() = booleanOrNull ?: throw IllegalStateException("Not a boolean")

  /**
   * Returns content of current element as boolean or `null` if current element is not a valid representation of boolean
   */
  override val booleanOrNull: Boolean? get() = literal as? Boolean

  override val numberOrNull: Number? get() = literal as? Number
  val number: Number get() = literal as? Number ?: throw NumberFormatException("Invalid number $contentOrNull")

  override fun toString() = content

  private fun mismatch(type: String): Nothing {
    throw IllegalStateException("json value is not a $type")
  }

  fun equalsLexically(other: Any?): Boolean {
    return when {
      this === other -> true
      other == null -> false
      other !is JsonLiteral -> false
      this.literal is Number && other.literal is Number -> contentOrNull == other.contentOrNull
      else -> literal == other.literal
    }
  }

  override fun equals(other: Any?): Boolean {
    return when {
      this === other -> true
      other == null -> false
      other !is JsonPrimitive -> false
      literal is Number && other.literal is Number -> literal.toDouble() == other.literal.toDouble()
      else -> literal == other.literal
    }
  }

  override fun hashCode(): Int {
    return literal?.hashCode() ?: 0
  }
}

/**
 * Class representing JSON literals: numbers, booleans and string.
 * Strings are always quoted.
 */
class JsonLiteral internal constructor(
    private val body: Any,
    override val content: String
) : JsonPrimitive(body) {

    override val contentOrNull: String = content

  /**
   * Creates number literal
   */
  constructor(number: Number) : this(number, number.toString())

  /**
   * Creates boolean literal
   */
  constructor(boolean: Boolean) : this(boolean, boolean.toString())

  /**
   * Creates quoted string literal
   */
  constructor(string: String) : this(string, string)

  val isString:Boolean = body is String

  override fun toString() =
      if (body is String) buildString { printQuoted(content) }
      else content

  // Compare by `content` and `isString`, because body can be kotlin.Long=42 or kotlin.String="42"
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as JsonLiteral

    if (body is String != other.body is String) return false
    if (content != other.content) return false

    return true
  }

  override fun hashCode(): Int {
    var result = (body is String).hashCode()
    result = 31 * result + content.hashCode()
    return result
  }
}

/**
 * Class representing JSON `null` value
 */
object JsonNull : JsonPrimitive(null) {
  override val jsonNull: JsonNull = this
  override val content: String = "null"
  override val contentOrNull: String? = null
}

/**
 * Class representing JSON object, consisting of name-value pairs, where value is arbitrary [JsonElement]
 */
data class JsonObject(val content: Map<String, JsonElement>) : JsonElement(), Map<String, JsonElement> by content {
  override val jsonObject: JsonObject = this

  /**
   * Returns [JsonElement] associated with given [key]
   * @throws NoSuchElementException if element is not present
   */
  override fun get(key: String): JsonElement = content[key]
      ?: throw NoSuchElementException("Element $key is missing")

  /**
   * Returns [JsonElement] associated with given [key] or `null` if element is not present
   */
  fun getOrNull(key: String): JsonElement? = content[key]

  /**
   * Returns [JsonPrimitive] associated with given [key]
   *
   * @throws NoSuchElementException if element is not present
   * @throws JsonElementTypeMismatchException if element is present, but has invalid type
   */
  fun getPrimitive(key: String): JsonPrimitive = get(key) as? JsonPrimitive
      ?: unexpectedJson(key, "JsonPrimitive")

  /**
   * Returns [JsonObject] associated with given [key]
   *
   * @throws NoSuchElementException if element is not present
   * @throws JsonElementTypeMismatchException if element is present, but has invalid type
   */
  fun getObject(key: String): JsonObject = get(key) as? JsonObject
      ?: unexpectedJson(key, "JsonObject")

  /**
   * Returns [JsonArray] associated with given [key]
   *
   * @throws NoSuchElementException if element is not present
   * @throws JsonElementTypeMismatchException if element is present, but has invalid type
   */
  fun getArray(key: String): JsonArray = get(key) as? JsonArray
      ?: unexpectedJson(key, "JsonArray")

  /**
   * Returns [JsonPrimitive] associated with given [key] or `null` if element
   * is not present or has different type
   */
  @Suppress("unchecked_cast")
  fun getPrimitiveOrNull(key: String): JsonPrimitive? = content[key] as? JsonPrimitive

  /**
   * Returns [JsonObject] associated with given [key] or `null` if element
   * is not present or has different type
   */
  fun getObjectOrNull(key: String): JsonObject? = content[key] as? JsonObject

  /**
   * Returns [JsonArray] associated with given [key] or `null` if element
   * is not present or has different type
   */
  fun getArrayOrNull(key: String): JsonArray? = content[key] as? JsonArray

  /**
   * Returns [J] associated with given [key]
   *
   * @throws NoSuchElementException if element is not present
   * @throws JsonElementTypeMismatchException if element is present, but has invalid type
   */
  inline fun <reified J : JsonElement> getAs(key: String): J = get(key) as? J
      ?: unexpectedJson(key, J::class.toString())

  /**
   * Returns [J] associated with given [key] or `null` if element
   * is not present or has different type
   */
  inline fun <reified J : JsonElement> lookup(key: String): J? = content[key] as? J

  override fun toString(): String {
    return content.entries.joinToString(
        prefix = "{",
        postfix = "}",
        transform = { (k, v) -> """"$k": $v""" }
    )
  }
}

data class JsonArray(val content: List<JsonElement>) : JsonElement(), List<JsonElement> by content {

  override val jsonArray: JsonArray = this

  /**
   * Returns [index]-th element of an array as [JsonPrimitive]
   * @throws JsonElementTypeMismatchException if element has invalid type
   */
  fun getPrimitive(index: Int) = content[index] as? JsonPrimitive
      ?: unexpectedJson("at $index", "JsonPrimitive")

  /**
   * Returns [index]-th element of an array as [JsonObject]
   * @throws JsonElementTypeMismatchException if element has invalid type
   */
  fun getObject(index: Int) = content[index] as? JsonObject
      ?: unexpectedJson("at $index", "JsonObject")

  /**
   * Returns [index]-th element of an array as [JsonArray]
   * @throws JsonElementTypeMismatchException if element has invalid type
   */
  fun getArray(index: Int) = content[index] as? JsonArray
      ?: unexpectedJson("at $index", "JsonArray")

  /**
   * Returns [index]-th element of an array as [JsonPrimitive] or `null` if element is missing or has different type
   */
  fun getPrimitiveOrNull(index: Int) = content.getOrNull(index) as? JsonPrimitive

  /**
   * Returns [index]-th element of an array as [JsonObject] or `null` if element is missing or has different type
   */
  fun getObjectOrNull(index: Int) = content.getOrNull(index) as? JsonObject

  /**
   * Returns [index]-th element of an array as [JsonArray] or `null` if element is missing or has different type
   */
  fun getArrayOrNull(index: Int) = content.getOrNull(index) as? JsonArray

  /**
   * Returns [index]-th element of an array as [J]
   * @throws JsonElementTypeMismatchException if element has invalid type
   */
  inline fun <reified J : JsonElement> getAs(index: Int): J = content[index] as? J
      ?: unexpectedJson("at $index", J::class.toString())

  /**
   * Returns [index]-th element of an array as [J] or `null` if element is missing or has different type
   */
  inline fun <reified J : JsonElement> getAsOrNull(index: Int): J? = content.getOrNull(index) as? J

  override fun toString() = content.joinToString(prefix = "[", postfix = "]")
}

@PublishedApi
internal fun unexpectedJson(key: String, expected: String): Nothing =
    throw JsonElementTypeMismatchException(key, expected)
