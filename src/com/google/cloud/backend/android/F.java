/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.backend.android;

import com.google.cloud.backend.android.mobilebackend.model.FilterDto;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A filter class for a {@link CloudQuery}. See
 * {@link CloudBackendTest#testList()} of CloudBackendAndroidClientTest project
 * for detailed usage.
 *
 */
public class F {

  public enum Op {
    EQ, LT, LE, GT, GE, NE, IN, AND, OR
  }

  private FilterDto filterDto = new FilterDto();

  /**
   * Creates a {@link F} for EQUAL operation.
   *
   * @param propertyName
   *          Name of the target property.
   * @param value
   *          Value for this operation.
   * @return {@link F} for this operation.
   */
  public static F eq(String propertyName, Object value) {
    return createFilter(Op.EQ.name(), propertyName, value);
  }

  /**
   * Creates a {@link F} for LESS_THAN operation.
   *
   * @param propertyName
   *          Name of the target property.
   * @param value
   *          Value for this operation.
   * @return {@link F} for this operation.
   */
  public static F lt(String propertyName, Object value) {
    return createFilter(Op.LT.name(), propertyName, value);
  }

  /**
   * Creates a {@link F} for LESS_THAN_EQUAL operation.
   *
   * @param propertyName
   *          Name of the target property.
   * @param value
   *          Value for this operation.
   * @return {@link F} for this operation.
   */
  public static F le(String propertyName, Object value) {
    return createFilter(Op.LE.name(), propertyName, value);
  }

  /**
   * Creates a {@link F} for GREATER_THAN operation.
   *
   * @param propertyName
   *          Name of the target property.
   * @param value
   *          Value for this operation.
   * @return {@link F} for this operation.
   */
  public static F gt(String propertyName, Object value) {
    return createFilter(Op.GT.name(), propertyName, value);
  }

  /**
   * Creates a {@link F} for GREATER_THAN_EQUAL operation.
   *
   * @param propertyName
   *          Name of the target property.
   * @param value
   *          Value for this operation.
   * @return {@link F} for this operation.
   */
  public static F ge(String propertyName, Object value) {
    return createFilter(Op.GE.name(), propertyName, value);
  }

  /**
   * Creates a {@link F} for NOT_EQUAL operation.
   *
   * @param propertyName
   *          Name of the target property.
   * @param value
   *          Value for this operation.
   * @return {@link F} for this operation.
   */
  public static F ne(String propertyName, Object value) {
    return createFilter(Op.NE.name(), propertyName, value);
  }

  /**
   * Creates a {@link F} for IN operation.
   *
   * @param propertyName
   *          Name of the target property.
   * @param values
   *          any number of {@link Object}s for the IN operation.
   * @return {@link F} for this operation.
   */
  public static F in(String propertyName, List<Object> values) {
    LinkedList<Object> l = new LinkedList<Object>(values);
    l.addFirst(propertyName);
    F f = new F();
    f.filterDto.setOperator(Op.IN.name());
    f.filterDto.setValues(l);
    return f;
  }

  /**
   * Creates a {@link F} for IN operation.
   *
   * @param propertyName
   *          Name of the target property.
   * @param values
   *          any number of {@link Object}s for the IN operation.
   * @return {@link F} for this operation.
   */
  public static F in(String propertyName, Object... values) {
    LinkedList<Object> l = new LinkedList<Object>(Arrays.asList(values));
    l.addFirst(propertyName);
    F f = new F();
    f.filterDto.setOperator(Op.IN.name());
    f.filterDto.setValues(l);
    return f;
  }

  /**
   * Creates a {@link F} for AND operation.
   *
   * @param filters
   *          Any number of {@link F}s for this operation.
   * @return {@link F} for this operation.
   */
  public static F and(F... filters) {
    F f = createFilterForAndOr(Op.AND.name(), filters);
    return f;
  }

  /**
   * Creates a {@link F} for OR operation.
   *
   * @param filters
   *          Any number of {@link F}s for this operation.
   * @return {@link F} for this operation.
   */
  public static F or(F... filters) {
    F f = createFilterForAndOr(Op.OR.name(), filters);
    return f;
  }

  private static F createFilterForAndOr(String op, F... filters) {
    F f = new F();
    f.filterDto.setOperator(op);
    List<FilterDto> subfilters = new LinkedList<FilterDto>();
    for (F cf : filters) {
      subfilters.add(cf.getFilterDto());
    }
    f.filterDto.setSubfilters(subfilters);
    return f;
  }

  protected static F createFilter(String op, String propertyName, Object value) {
    F f = new F();
    f.filterDto.setOperator(op);
    List<Object> values = new LinkedList<Object>();
    values.add(propertyName);
    values.add(value);
    f.filterDto.setValues(values);
    return f;
  }

  public FilterDto getFilterDto() {
    return filterDto;
  }

  @Override
  public String toString() {
    return "F: op: " + this.filterDto.getOperator() + ", values: " + this.filterDto.getValues();
  }

}
